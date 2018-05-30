(ns flexblock.db
  "Functions that interact with the database."
  (:require [korma.core :refer :all]
            [korma.db :refer :all]
            [heroku-database-url-to-jdbc.core :as heroku]
            [honeysql.core :as sql]
            [buddy.hashers :as h]
            [clojure.string :as str]
            [clojure.core.async :as a]
            [flexblock.rooms :as r]
            [flexblock.users :as u]
            [flexblock.notifier :as n]
            [flexblock.config :refer [env]]
            [flexblock.migrations :as migrations]
            [mount.core :as mount]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :as time]))

(declare rooms)

(defentity users-rooms
  (table :users_rooms))

(defentity users
  (entity-fields :id :name :email :teacher :advisor_id :admin)
  (many-to-many rooms :users_rooms))

(defentity rooms
  (many-to-many users :users_rooms))

(defn init-seed-user! [user]
  (if (empty? (select users (where {:email (:email user)})))
    (let [password-hash (h/derive (:password user))]
      (insert users
              (values (-> user
                          (dissoc :password)
                          (assoc :passwordhash password-hash)))))))

(heroku/korma-connection-map nil)

(mount/defstate db
  :start (let [connection-or-url (or (get-in env [:database-url])
                                     (get-in env [:db :connection]))
               connection        (if (string? connection-or-url)
                                   (heroku/korma-connection-map
                                    connection-or-url)
                                   connection-or-url)
               db-info           (merge connection
                                        {:naming
                                         {:keys
                                          #(str/replace % #"_" "-")}})
               seed-user         (get-in env [:db :seed-user])]
           (if db-info
             (do
               ;; Set default connection for Korma.
               (default-connection (create-db db-info))
               ;; Create tables if they don't already exist.
               (migrations/init-tables! db-info)
               ;; Add in a seed user if provided in config.
               (if seed-user
                 (init-seed-user! seed-user))
               db-info)
             (throw (Exception. "Invalid DB info.")))))


(defn get-advisor [id]
  (or (first (jdbc/query db (sql/format
                             {:select [[:a.name :advisor]]
                              :from   [[:users :u]]
                              :join   [[:users :a]
                                       [:= :a.id :u.advisor_id]]
                              :where  [:= :u.id id]})))
      ""))

(defn school-year []
  (let [time  (time/now)
        year  (time/year time)
        month (time/month time)]
    (if (>= month 7)
      (inc year)
      year)))

(defn get-users
  "Get all users saved in the database."
  []
  (map #(merge % (get-advisor (:id %)))
       (select users
               (where (or {:class [= nil]}
                          {:class [>= (school-year)]}))
               (with rooms
                     (with users
                           (where {:teacher true}))))))

(defn get-user
  "Get one user by `id`."
  [id]
  (first (select users
                 (where {:id id}))))

(defn insert-user!
  [user-id email password name teacher? admin? class advisor-id]
  (let [creator (get-user user-id)
        user    {:email        email
                 :passwordhash (h/derive password)
                 :name         name
                 :teacher      teacher?
                 :admin        admin?
                 :class        class
                 :advisor_id   advisor-id}]
    (if-not (u/can-edit? creator user)
      (throw (ex-info nil {:message
                           "You don't have permission to do that."}))
      (let [new-user (insert users (values user))]
        (a/put! n/notifier {:event     :user/create
                            :recipient new-user
                            :password  password})))))

(defn get-rooms
  "Get all rooms saved in the database."
  []
  (select rooms
          (with users)))

(defn get-room
  "Get one room by `id`."
  [id]
  (first (select rooms
                 (where {:id id})
                 (with users))))

(defn insert-room!
  "Inserts a room into the database.
  Also adds an entry to users-rooms between the new room and its creator."
  [creator-id title description date time room-number max-capacity]
  (transaction
   (if (:teacher (get-user creator-id))
     (let [room-id (:id (insert rooms
                                (values {:title        title
                                         :description  description
                                         :date         (java.sql.Date.
                                                        (inst-ms date))
                                         :time         time
                                         :room_number  room-number
                                         :max_capacity max-capacity})))]
       (insert users-rooms
               (values {:users_id creator-id
                        :rooms_id room-id})))
     (throw (ex-info nil {:message "Only teachers can create rooms"})))))

(defn delete-room!
  "Deletes a room, given a `room-id` and a `user-id`.
  The `user-id` will be used to check that the user has the correct
  permissions to delete the room."
  [user-id room-id]
  (transaction
   (let [room    (get-room room-id)
         teacher (r/get-teacher room)]
     (if-not (= user-id (:id teacher))
       (throw (ex-info nil {:message
                            "Only the creator of a room can delete it."}))
       (do (delete users-rooms
                   (where {:rooms_id room-id}))
           (delete rooms
                   (where {:id room-id}))
           (doseq [recipient (:users room)]
             (a/put! n/notifier {:event     :room/delete
                                 :recipient recipient
                                 :room      room})))))))

(defn join-room
  "Creates a users-rooms relationship between `user-id` and `room-id`."
  [user-id room-id]
  (transaction
   (let [room   (get-room room-id)
         joined (->> room :users (remove :teacher) count)
         user   (get-user user-id)]
     (cond
       (nil? room)
       (throw (ex-info nil {:message "Room does not exist."}))

       (:teacher user)
       (throw (ex-info nil {:message "Teachers cannot join rooms."}))

       (r/in-room? room user-id)
       (throw (ex-info nil {:message "You are already in this room."}))

       (>= joined
           (:max-capacity room))
       (throw (ex-info nil {:message "This room is full."}))

       :else
       (do
         (insert users-rooms
                 (values {:users_id user-id
                          :rooms_id room-id}))
         (doseq [recipient [user (r/get-teacher room)]]
           (a/put! n/notifier {:event     :room/join
                               :recipient recipient
                               :user      user
                               :room      room})))))))

(defn leave-room
  "Removes a users-rooms relationship between `user-id` and `room-id`."
  [user-id room-id]
  (transaction
   (let [room   (get-room room-id)
         joined (->> room :users (remove :teacher) count)
         user   (get-user user-id)]
     (cond
       (nil? room)
       (throw (ex-info nil {:message "Room does not exist."}))

       (:teacher user)
       (throw (ex-info nil {:message "Teachers cannot leave rooms."}))

       (not (r/in-room? room user-id))
       (throw (ex-info nil {:message "You are not in this room."}))

       :else
       (do
         (delete users-rooms
                 (where {:users_id user-id
                         :rooms_id room-id}))
         (doseq [recipient [user (r/get-teacher room)]]
           (a/put! n/notifier {:event     :room/leave
                               :recipient recipient
                               :user      user
                               :room      room})))))))

(defn check-login
  "Checks if `password` is valid for a user with `email`."
  [email password]
  (if-let [user (first (select users
                               (fields :id :name :email :teacher :passwordhash)
                               (where {:email email})
                               (limit 1)))]
    (if (h/check (str password) (:passwordhash user))
      (-> user (dissoc :passwordhash))
      false)
    false))


(defn set-password [password user-id setter-id]
  (let [user   (get-user user-id)
        setter (get-user setter-id)]
    (cond
      (not (and user setter))
      (throw (ex-info nil {:message "User does not exist!"}))

      (not (u/can-edit? setter user))
      (throw
       (ex-info nil
                {:message
                 (format
                  "You don't have permission to set %s's password."
                  (:name user))}))

      :else
      (do (update users
                  (set-fields {:passwordhash (h/derive password)})
                  (where {:id (:id user)}))
          (a/put! n/notifier {:event     :user/set-password
                              :recipient user
                              :user      setter})))))

(ns flexblock.db
  "Functions that interact with the database."
  (:require [korma.core :refer :all]
            [korma.db :refer :all]
            [honeysql.core :as sql]
            [buddy.hashers :as h]
            [clojure.string :as str]
            [clojure.core.async :as a]
            [flexblock.rooms :as r]
            [flexblock.users :as u]
            [flexblock.notifier.core :as n]
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

(defn start-db
  "This function is responsible for starting the database.
  Takes `env` as an argument. `env` is cannonically
  `flexblock.config/env`, but can be swapped for any associative data
  structure that contains the required keys.

  First, `start-db` looks for a :jdbc-database-url key in `env`. This
  beavior allows for zero configuration when run on Heroku, as Heroku
  automatically sets this key in the environment variable. This key
  should contain a valid JDBC url.

  If a :jdbc-database-url key is not found in `env`, `start-db` looks
  for the key :database in `env`. If the :database key is found, it
  will look for a nested :spec key. This key should contain a full
  JDBC database spec, which will be used to connect to the database.

  If neither key exists, an in-memory database will be created for
  development and testing. Note, the state of the in-memory db will be
  discarded after `db` is stopped.

  This function should create a connection pool using korma, set it as
  the default connection for korma, and return it, potentially to be
  used by non-korma consumers, such as in `flexblock.migrations`.

  This function will also look for a seed user that will be added to
  the database on startup. The seed user should be stored under the
  key :seed-user, nested inside the :database key. If no seed user is
  provided, it will create a default one, with the email
  \"example@example.com\" and the password \"password\"."
  [env]
  (let [jdbc-url  (get-in env [:jdbc-database-url])
        jdbc-spec (get-in env [:database :spec])
        seed-user (or (get-in env [:database :seed-user])
                      ;; Default seed user.
                      {:name     "Example Admin"
                       :email    "example@example.com"
                       :password "password"
                       :teacher  false
                       :admin    true})
        ;; Select the correct db-spec.
        db        (merge
                   (cond
                     jdbc-url  {:connection-uri jdbc-url}
                     jdbc-spec jdbc-spec
                     :else     {:dbtype     "h2:mem"
                                :dbname     "flexblockdb"
                                :delimiters ""})
                   ;; Clojure-ify db keys.
                   {:naming
                    {:keys
                     #(str/lower-case (str/replace % #"_" "-"))}})]

    ;; Set the default connection for Korma.
    (default-connection (create-db db))
    ;; Initialize tabes, if they aren't already set up.
    (migrations/init-tables! db)
    ;; Add the seed user, if they don't already exist.
    (init-seed-user! seed-user)
    db))

(mount/defstate db
  "Manages the state of the db. Calls `start-db` on startup. See the
  docstrings of these functions for more details on how the database
  works."
  :start (start-db env))

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
                            :recipient user
                            :password  password})))))

(defn delete-user!
  "Removes a user from the database.
  Takes `user-id`, the ID of the user to be removed, and `deleter-id`,
  the ID of the user performing the delete operation."
  [user-id deleter-id]
  (let [user    (get-user user-id)
        deleter (get-user deleter-id)]
    (cond
      (not (and user deleter))
      (throw
       (ex-info nil
                {:message "User does not exist!"}))

      (not (u/can-delete? deleter user))
      (throw
       (ex-info nil
                {:message
                 (format
                  "You don't have permission to delete %s's account."
                  (:name user))}))

      :else (delete users
                    (where {:id user-id})))))

(defn get-attendance
  [user-id room-id]
  (:attendance (first
                (select users-rooms
                        (where {:users_id user-id
                                :rooms_id room-id})))))


(defn get-rooms
  "Get all rooms saved in the database."
  []
  (let [rooms (select rooms (with users))]
    (map (fn [room]
           (assoc room :users
                  (map #(assoc %
                               :attendance
                               (get-attendance (:id %) (:id room)))
                       (:users room))))
         rooms)))


(defn get-room
  "Get one room by `id`."
  [id]
  (first (select rooms
                 (where {:id id})
                 (with users))))

(defn set-attendance
  "Sets the attendance of a user for room.
  Attendance should be an integer:
  -1 Absent
   0 Undefined
   1 Present"
  [room-id user-id setter-id attendance]
  (let [room   (get-room room-id)
        user   (get-user user-id)
        setter (get-user setter-id)]
    (cond
      (not (and user setter))
      (throw (ex-info nil {:message "User does not exist!"}))

      (not room)
      (throw (ex-info nil {:message "Room does not exist!"}))

      (not (u/can-edit? setter user))
      (throw
       (ex-info nil
                {:message
                 (format
                  "You don't have permission to set %s's attendance."
                  (:name user))}))

      :else
      (update users-rooms
              (set-fields {:attendance attendance})
              (where {:rooms_id room-id
                      :users_id user-id})))))

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
               (values {:attendance 0
                        :users_id   creator-id
                        :rooms_id   room-id})))
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
                 (values {:attendance 0
                          :users_id   user-id
                          :rooms_id   room-id}))
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
      (dissoc user :passwordhash)
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

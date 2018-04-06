(ns flexblock.db
  "Functions that interact with the database."
  (:require [korma.core :refer :all]
            [korma.db :refer :all]
            [buddy.hashers :as h] 
            [clojure.core.async :as a]
            [flexblock.rooms :as r]
            [flexblock.notifier :as n]
            [flexblock.config :refer [env]] 
            [mount.core :as mount]))

(defdb db (postgres {:db       (get (System/getenv) "db__db")
                     :user     (get (System/getenv) "db__user")
                     :password (get (System/getenv) "db__password")}))

(declare rooms)

(defentity users-rooms
  (table :users_rooms))

(defentity users
  (entity-fields :id :name :email :teacher :advisor) 
  (many-to-many rooms :users_rooms))

(defentity rooms
  (many-to-many users :users_rooms))

(defn get-users
  "Get all users saved in the database."
  []
  (select users
          (with rooms
                (with users
                      (where {:teacher true})))))

(defn get-user
  "Get one user by `id`."
  [id]
  (first (select users
                 (where {:id id}))))

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
                                         :date         date
                                         :time         time
                                         :room_number  room-number
                                         :max_capacity max-capacity})))]
       (insert users-rooms
               (values {:users_id creator-id
                        :rooms_id room-id})))
     "Only teachers can create rooms")))

(defn delete-room!
  "Deletes a room, given a `room-id` and a `user-id`.
  The `user-id` will be used to check that the user has the correct
  permissions to delete the room."
  [user-id room-id]
  (transaction
   (let [room    (get-room room-id)
         teacher (r/get-teacher room)]
     (if-not (= user-id (:id teacher))
       "Only the creator of a room can delete it."
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
       "Room does not exist."

       (:teacher user)
       "Teachers cannot join rooms." 

       (r/in-room? room user-id)
       "You are already in this room."

       (>= joined
           (:max_capacity room))
       "This room is full."

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
       "Room does not exist."

       (:teacher user)
       "Teachers cannot leave rooms." 

       (not (r/in-room? room user-id))
       "You are not in this room." 

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
      "The user could not be found."
      
      (not (:teacher setter))
      "Only a teacher can set a password"

      (:teacher user)
      "Only a student's password can be set"

      :else
      (do (update users
                  (set-fields {:passwordhash (h/derive password)})
                  (where {:id (:id user)}))
          (a/put! n/notifier {:event     :user/set-password
                              :recipient user
                              :user      setter})))))

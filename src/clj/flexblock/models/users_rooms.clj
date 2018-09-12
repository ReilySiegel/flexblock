(ns flexblock.models.users-rooms
  (:require [clojure.core.async :as a]
            [flexblock.models.helpers :as helpers :refer [*master* ex-info-assert]]
            [flexblock.models.room :refer [Room]]
            [flexblock.models.user :refer [User]]
            [flexblock.notifier.core :as n]
            [flexblock.rooms :as rooms]
            [flexblock.users :as users]
            [toucan.db :as db]
            [toucan.hydrate :as hydrate]
            [toucan.models :as models]))

(models/defmodel UsersRooms :users-rooms)

(defn pre-insert [users-rooms]
  (let [room   (hydrate/hydrate
                (db/select-one Room :id (:rooms-id users-rooms))
                :users)
        joined (->> room :users (remove :teacher) count)
        user   (db/select-one User :id (:users-id users-rooms))]
    ;; Assert that the room and user exist.
    (ex-info-assert room "Room does not exist.")
    (ex-info-assert user "User does not exist.")
    ;; Assert that the room is not full.
    (ex-info-assert (< joined (:max-capacity room))
                    "This room is full.")
    ;; Assert that the user is not a teacher. This will not be called
    ;; on initial room creation, as the teacher is added with
    ;; `insert-simple!`.
    (ex-info-assert (not (:teacher user))
                    "Teachers cannot join rooms.")
    ;; Assert that the user has not already joined the room.
    (ex-info-assert (not (rooms/in-room? room (:id user)))
                    "You are already in this room.")
    users-rooms))

(defn post-insert [users-rooms]
  (let [room    (-> (db/select-one Room :id (:rooms-id users-rooms))
                    (hydrate/hydrate :users))
        user    (db/select-one User :id (:users-id users-rooms))
        teacher (rooms/get-teacher room)]
    (doseq [recipient [user teacher]]
      (a/put! n/notifier {:event     :room/leave
                          :recipient recipient
                          :user      user
                          :room      room}))))

(defn pre-update [users-rooms]
  (let [user (db/select-one User :id (:users-id users-rooms))]
    (helpers/assert-master)
    ;; Assert that master can edit user.
    (ex-info-assert (users/can-edit? *master* user)
                    "You don't have permission to edit this user.")
    users-rooms))

(defn pre-delete [users-rooms]
  (let [room   (hydrate/hydrate
                (db/select-one Room :id (:rooms-id users-rooms))
                :users)
        joined (->> room :users (remove :teacher) count)
        user   (db/select-one User :id (:users-id users-rooms))]
    ;; Assert that the user is not a teacher. This will not be called
    ;; on initial room creation, as the teacher is added with
    ;; `insert-simple!`.
    (ex-info-assert (not (:teacher user))
                    "Teachers cannot leave rooms.")))

(defn post-delete [users-rooms]
  (let [room    (-> (db/select-one Room :id (:rooms-id users-rooms))
                    (hydrate/hydrate :users))
        user    (db/select-one User :id (:users-id users-rooms))
        teacher (rooms/get-teacher room)]
    (doseq [recipient [user teacher]]
      (a/put! n/notifier {:event     :room/join
                          :recipient recipient
                          :user      user
                          :room      room}))))


(extend (class UsersRooms)
  models/IModel
  (merge models/IModelDefaults
         {:pre-insert  pre-insert
          :post-insert post-insert
          :pre-update  pre-update
          :pre-delete  pre-delete
          :post-delete post-delete}))

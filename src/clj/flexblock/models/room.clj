(ns flexblock.models.room
  (:require [clj-time.coerce :as timec]
            [clojure.core.async :as a]
            [clojure.spec.alpha :as s]
            [flexblock.models.helpers :as helpers :refer [*master* ex-info-assert]]
            [flexblock.notifier.core :as n]
            [flexblock.rooms :as rooms]
            [phrase.alpha :as phrase]
            [toucan.db :as db]
            [toucan.hydrate :as hydrate]
            [toucan.models :as models]))

(models/defmodel Room :rooms)

(defn pre-insert [room]
  (helpers/assert-master)
  ;; Assert that the room is valid.
  (ex-info-assert (s/valid? ::rooms/room room)
                  (phrase/phrase-first {} ::rooms/room room))
  (ex-info-assert (:teacher *master*)
                  "Only teachers can create Sessions.")
  (update room :date #(timec/to-sql-date (timec/from-date %))))

(defn post-insert [room]
  (db/simple-insert! 'UsersRooms
    {:users-id   (:id *master*)
     :rooms-id   (:id room)
     :attendance 0})
  room)

(defn pre-delete [room]
  (let [room-h  (hydrate/hydrate room :users)
        teacher (rooms/get-teacher room-h)]
    (helpers/assert-master)
    ;; Assert that master is the owner of the room.
    (ex-info-assert (= (:id *master*) (:id teacher))
                    "You can't delete this room.")
    ;; Remove all students from the room, sending a delete-specific
    ;; notification.
    (doseq [user (:users room-h)]
      (db/simple-delete! 'UsersRooms
        :rooms-id (:id room)
        :users-id (:id user))
      (a/put! n/notifier {:event     :room/delete
                          :recipient user
                          :room      room}))
    room))

(defn- users-for-room
  "Takes a seq of `user-ids`, a seq of `rooms`, and a seq of
  `users-rooms` maps. Returns a map of user-ids to rooms."
  [room-ids users users-rooms]
  (let [user-id->user     (into {} (for [user users] [(:id user) user]))
        room-id->user-ids (apply merge-with concat
                                 (for [user-room users-rooms]
                                   (if (contains? user-id->user
                                                  (:users-id user-room))
                                     {(:rooms-id user-room)
                                      [(:users-id user-room)]})))]
    (into {}
          (for [room-id room-ids]
            [room-id
             (map #(get user-id->user %)
                  (get room-id->user-ids room-id))]))))

(defn ^:batched-hydrate users
  [rooms]
  (let [room-ids       (map :id rooms)
        users-rooms    (db/select
                           'UsersRooms :rooms-id [:in room-ids])
        user-ids       (map :users-id users-rooms)
        users          (db/select 'User :id   [:in user-ids])
        room-id->users (users-for-room room-ids users users-rooms)]
    (for [room rooms]
      (assoc room :users (get room-id->users (:id room))))))

(extend (class Room)
  models/IModel
  (merge models/IModelDefaults
         {:hydration-keys (constantly [:users])
          :pre-insert     pre-insert
          :post-insert    post-insert
          :pre-delete     pre-delete
          :types          (constantly {:time :keyword})}))

(ns flexblock.models.room
  (:require [clj-time.coerce :as timec]
            [clojure.core.async :as a]
            [clojure.spec.alpha :as s]
            [flexblock.models.helpers :as helpers :refer [*master* ex-info-assert]]
            [flexblock.notifier.core :as n]
            [flexblock.rooms :as rooms]
            [flexblock.search :as search]
            [phrase.alpha :as phrase]
            [toucan.db :as db]
            [toucan.hydrate :as hydrate]
            [toucan.models :as models]))

(models/defmodel Room :rooms)

;;; Hooks

(defn pre-insert [room]
  (helpers/assert-master)
  ;; Assert that the room is valid.
  (ex-info-assert (s/valid? ::rooms/room room)
                  (or (phrase/phrase-first {} ::rooms/room room)
                      "Please fill out all required fields."))
  (ex-info-assert (:teacher *master*)
                  "Only teachers can create Sessions.")
  (assoc room :tokens (rooms/tokenize room)))

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
    (ex-info-assert (or (= (:id *master*) (:id teacher))
                        (:admin *master*))
                    "You can't delete this room.")
    ;; Remove all students from the room, sending a delete-specific
    ;; notification.
    (doseq [user (:users room-h)]
      (db/simple-delete! 'UsersRooms
        :rooms-id (:id room)
        :users-id (:id user))
      (a/put! n/notifier {:event     :room/delete
                          :recipient user
                          :room      room-h}))
    room))

;;; Hydration

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

(models/add-type! :date
  :in  timec/to-sql-date
  :out timec/to-date)

(extend (class Room)
  models/IModel
  (merge models/IModelDefaults
         {:hydration-keys (constantly [:users])
          :pre-insert     pre-insert
          :post-insert    post-insert
          :pre-delete     pre-delete
          :types          (constantly {:time   :keyword
                                       :date   :date
                                       :tokens :edn})}))

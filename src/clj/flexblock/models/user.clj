(ns flexblock.models.user
  (:require [buddy.hashers :as h]
            [clj-time.coerce :as timec]
            [clj-time.core :as time]
            [clojure.core.async :as a]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [flexblock.models.helpers
             :as helpers
             :refer [*master* ex-info-assert]]
            [flexblock.notifier.core :as n]
            [flexblock.users :as users]
            [phrase.alpha :as phrase]
            [toucan.db :as db]
            [toucan.models :as models]
            [toucan.hydrate :as hydrate]))

(models/defmodel User :users)

;; Stores the password for a new user, so that it can be accessed in
;; post-insert.
(def ^:dynamic *password*)

(def defaults {:teacher false
               :admin   false})

(defn pre-delete [user]
  (helpers/assert-master)
  ;; Assert that *master* can delete user.
  (ex-info-assert (users/can-delete? *master* user)
                  (format "You don't have permission to delete %s."
                          (:name user)))
  (if-not (:teacher user)
    ;; If the user is not a teacher, remove them from all rooms.
    (db/simple-delete! 'UsersRooms :users-id (:id user))
    (let [room-ids (try (db/select-field :rooms-id 'UsersRooms
                          :users-id (:id user))
                        (catch Exception e []))
          rooms    (when (seq room-ids)
                     (db/select 'Room :id [:in room-ids]))]
      ;; Assert that all rooms are in the past or have no users.
      (ex-info-assert (every? #(or
                                (time/before? (timec/from-date (:date %))
                                              ;; Time at midnight.
                                              (time/today-at 0 0))
                                ;; Only the teacher is in users.
                                (= 1 (count (:users %))))
                              (hydrate/hydrate rooms :users))
                      "This teacher currently has open rooms.")
      (when (seq room-ids)
        (db/simple-delete! 'UsersRooms :rooms-id [:in room-ids])
        (db/simple-delete! 'Room :id [:in room-ids]))))
  user)



(defn pre-update [user]
  ;; Get all user fields
  (let [new-user? (nil? (:id user))
        user      (merge (db/select-one User :id (:id user))
                         user)]
    (helpers/assert-master)
    ;; Assert that master is allowed to edit user.
    (ex-info-assert (users/can-edit? *master* user)
                    "You don't have permission to edit this user.")
    (when new-user?
      (ex-info-assert (not (db/exists? User :email (:email user)))
                      "A user with this email already exists."))
    (when (and (not new-user?) (:password user))
      (a/put! n/notifier
              {:event     :user/set-password
               :recipient user
               :user      *master*}))
    (-> user
        (merge
         (if (contains? user :password)
           {:passwordhash
            (h/derive (:password user))}))
        (dissoc :password)
        (assoc :tokens (users/tokenize user)))))

(defn pre-insert [user]
  (let [user (merge
              defaults user
              (if (and (= :student (users/highest-role user))
                       (:teacher *master*))
                {:advisor-id (:id *master*)}))]
    ;; Assert that the user is valid.
    (ex-info-assert (s/valid? ::users/user user)
                    (phrase/phrase-first {} ::users/user user))
    ;; Assert that a password is provided.
    (ex-info-assert (:password user)
                    "User does not contain password.")
    ;; Run all pre-update checks.
    (pre-update user)))

(defn post-insert [user]
  (a/put! n/notifier {:event     :user/create
                      :recipient user
                      :password  *password*})
  user)

(defn ^:batched-hydrate advisor-name
  [users]
  (let [advisor-ids (set (map :advisor-id users))
        id->name    (db/select-id->field :name User :id [:in advisor-ids])]
    (for [user users]
      (assoc user :advisor-name (get id->name (:advisor-id user))))))

(defn- rooms-for-user
  "Takes a seq of `user-ids`, a seq of `rooms`, and a seq of
  `users-rooms` maps. Returns a map of user-ids to rooms."
  [user-ids rooms users-rooms]
  (let [room-id->rooms    (into {} (for [room rooms] [(:id room) room]))
        user-id->room-ids (apply merge-with concat
                                 (for [user-room users-rooms]
                                   (if (contains? room-id->rooms
                                                  (:rooms-id user-room))
                                     {(:users-id user-room)
                                      [(:rooms-id user-room)]})))]
    (into {}
          (for [user-id user-ids]
            [user-id
             (map #(get room-id->rooms %)
                  (get user-id->room-ids user-id))]))))

(defn ^:batched-hydrate rooms
  [users]
  (try
    (let [user-ids       (map :id users)
          users-rooms    (try  (db/select
                                   'UsersRooms :users-id [:in user-ids])
                               (catch Exception e []))
          room-ids       (map :rooms-id users-rooms)
          rooms          (when (seq room-ids)
                           (db/select 'Room
                             :id   [:in room-ids]
                             :date [:>= (timec/to-sql-date
                                         (time/minus
                                          (time/today)
                                          (time/weeks 1)))]))
          user-id->rooms (rooms-for-user user-ids rooms users-rooms)]
      (for [user users]
        (assoc user :rooms (get user-id->rooms (:id user)))))
    (catch Exception e
      (log/error e "Error hydrating rooms.")
      users)))

(extend (class User)
  models/IModel
  (merge
   models/IModelDefaults
   {:default-fields
    (constantly [:id :name :email :class :advisor-id :teacher :admin :tokens])

    :hydration-keys
    (constantly [:advisor :advisor-name :rooms])
    :pre-update  pre-update
    :pre-delete  pre-delete
    :pre-insert  pre-insert
    :post-insert post-insert
    :types       (constantly {:tokens :edn})}))

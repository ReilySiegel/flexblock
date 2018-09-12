(ns flexblock.models.user
  (:require [buddy.hashers :as h]
            [clj-time.coerce :as timec]
            [clj-time.core :as time]
            [clojure.core.async :as a]
            [clojure.spec.alpha :as s]
            [flexblock.models.helpers
             :as helpers
             :refer [*master* ex-info-assert]]
            [flexblock.notifier.core :as n]
            [flexblock.users :as users]
            [phrase.alpha :as phrase]
            [toucan.db :as db]
            [toucan.models :as models]))

(models/defmodel User :users)

;; Stores the password for a new user, so that it can be accessed in
;; post-insert.
(def ^:dynamic *password*)

(defn pre-delete [user]
  (helpers/assert-master)
  ;; Assert that *master* can delete user.
  (ex-info-assert (users/can-delete? *master* user)
                  (format "You don't have permission to delete %s."
                          (:name user)))
  user)


(defn pre-update [user]
  (helpers/assert-master)
  ;; Assert that master is allowed to edit user.
  (ex-info-assert (users/can-edit? *master* user)
                  "You don't have permission to edit this user.")
  (ex-info-assert (not (db/exists? User :email (:email user)))
                  "A user with this email already exists.")
  (-> user
      (merge
       (if (contains? user :password)
         {:passwordhash
          (h/derive (:password user))}))
      (dissoc :password)))

(defn pre-insert [user]
  ;; Assert that the user is valid.
  (ex-info-assert (s/valid? ::users/user user)
                  (phrase/phrase-first {} ::users/user user))
  ;; Assert that a password is provided.
  (ex-info-assert (:password user)
                  "User does not contain password.")
  (merge
   ;; Run all pre-update checks.
   (pre-update user)
   (if (= :student (users/highest-role user))
     {:advisor-id (:id *master*)})))

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
  (let [user-ids       (map :id users)
        users-rooms    (db/select
                           'UsersRooms :users-id [:in user-ids])
        room-ids       (map :rooms-id users-rooms)
        rooms          (db/select 'Room
                         :id   [:in room-ids]
                         :date [:>= (timec/to-sql-date
                                     (time/minus
                                      (time/today)
                                      (time/weeks 1)))])
        user-id->rooms (rooms-for-user user-ids rooms users-rooms)]
    (for [user users]
      (assoc user :rooms (get user-id->rooms (:id user))))))

(extend (class User)
  models/IModel
  (merge
   models/IModelDefaults
   {:default-fields
    (constantly [:id :name :email :class :advisor-id :teacher :admin])

    :hydration-keys
    (constantly [:advisor :advisor-name :rooms])
    :pre-update  pre-update
    :pre-insert  pre-insert
    :post-insert post-insert}))

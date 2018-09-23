(ns flexblock.db
  "Public Database API.
  These are the database-related functions that are made available to
  other parts of the code base, such as `flexblock.routes` Most
  functions in this namespace should be pretty simple, and mostly
  consist of calling into `toucan` functions. The logic behind the
  database is located in the `flexblock.models` namespaces. All
  business logic, such as input validation and notification sending
  should be done from there."
  (:require [buddy.hashers :as h]
            [clj-time.coerce :as timec]
            [clj-time.core :as time]
            [flexblock.config :refer [env]]
            [flexblock.migrations :as migrations]
            [flexblock.models.helpers :refer [*master*]]
            [flexblock.models.room :refer [Room]]
            [flexblock.models.user :refer [*password* User]]
            [flexblock.models.users-rooms :refer [UsersRooms]]
            [flexblock.users :as users]
            [mount.core :as mount]
            [toucan.db :as db]
            [toucan.hydrate :as hydrate]
            [toucan.models :as models]))

;;;; This section is responsible for setting up a database connection.
;;;; Things like database settings, delimiters, and connections are
;;;; managed in this section.

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
        db        (cond
                    jdbc-url  {:connection-uri jdbc-url}
                    jdbc-spec jdbc-spec
                    :else     {:dbtype     "h2:mem"
                               :dbname     "flexblockdb"
                               :delimiters :mysql})]

    ;; Set the default connection for Toucan.
    (db/set-default-db-connection! db)
    (db/set-default-automatically-convert-dashes-and-underscores! true)
    (db/set-default-quoting-style! (:delimiters db :ansi))
    (models/set-root-namespace! 'flexblock.models)
    ;; Set the default timezone.
    (java.util.TimeZone/setDefault (java.util.TimeZone/getTimeZone "UTC"))
    ;; Initialize tabes, if they aren't already set up.
    (migrations/init-tables! db)
    ;; Add the seed user, if they don't already exist.
    (when-not (db/exists? User :email (:email seed-user))
      (db/simple-insert! User
        (-> seed-user
            (assoc :passwordhash
                   (h/derive (:password seed-user)))
            (dissoc :password))))
    db))

(mount/defstate db
  "Manages the state of the db. Calls `start-db` on startup. See the
  docstrings of these functions for more details on how the database
  works."
  :start (start-db env))

;;;; The following section relates to users.
;;;;
;;;; Details such as hydration, error checking, permission validation,
;;;; password hashing and notification delivery are implemented in
;;;; `flexblock.models.room`.

(defn get-room
  "Returns one room, with the given `id`, hydrated with a list of users."
  [id]
  (hydrate/hydrate
   (db/select-one Room :id id)
   :users))

(defn get-rooms
  "Returns a list of all rooms, hydrated with a list of users."
  []
  (hydrate/hydrate
   (db/select Room :date [:>= (timec/to-sql-date
                               (time/today))])
   :users))

(defn insert-room!
  "Takes a `room` and a `master-id`. Inserts the room into the database."
  [room master-id]
  (binding [*master* (db/select-one User :id master-id)]
    (db/insert! Room room)))

(defn delete-room!
  "Takes a `room-id` and a `master-id`. Deletes the room from the
  database."
  [room-id master-id]
  (binding [*master* (db/select-one User :id master-id)]
    (db/delete! Room :id room-id)))

;;;; The following section relates to users.
;;;;
;;;; Details such as hydration, error checking, permission validation,
;;;; password hashing and notification delivery are implemented in
;;;; `flexblock.models.user`.

(defn school-year
  "Returns the current school year, defined July to July.

  Example:
  Current date: 2018-03-01
  School year: 2018

  Current date: 2018-09-01
  School year: 2019"
  []
  (let [time  (time/now)
        year  (time/year time)
        month (time/month time)]
    (if (>= month 7)
      (inc year)
      year)))

(defn get-users
  "Wrapper to get all users, which hydrates related data.
  Does not select users whose :class is less than `school-year`"
  []
  (hydrate/hydrate
   (db/select User {:where [:or
                            [:= :class nil]
                            [:>= :class (school-year)]]})
   :advisor-name :rooms))

(defn get-user
  "Wrapper to get user by :id, hydrating related fields."
  [id]
  (hydrate/hydrate (User :id id) :advisor-name :rooms))

(defn insert-user!
  "Simple wrapper to insert user.
  Takes a `user` to insert, and a `master-id` responsible for the
  insertion."
  [user master-id]
  (binding [*password* (:password user (users/gen-password 16))
            *master*   (db/select-one User :id master-id)]
    (db/insert! User (assoc user :password *password*))))

(defn delete-user!
  "Simple wrapper to delete user.
  Takes a `user-id` to delete, and a `master-id` responsible for the
  insertion."
  [user-id master-id]
  (binding [*master* (db/select-one User :id master-id)]
    (db/delete! User :id user-id)))

(defn check-login
  "Takes an `email` and a `password`. Returns the user with `email` if
  the login is valid, otherwise returns false."
  [email password]
  (let [passwordhash (db/select-one-field :passwordhash User
                       :email email)
        user         (db/select-one User :email email)]
    (if (and passwordhash
             (h/check password  passwordhash))
      user
      false)))

(defn set-password!
  [user-id master-id password]
  (binding [*master* (db/select-one User :id master-id)]
    (db/update! User user-id {:password password})))

;;;; The following section implements UsersRooms functions.
;;;;
;;;; Validation and notification delivery are handled in
;;;; `flexblock.models.users-rooms`.

(defn get-attendance
  "Gets attendance values for all rooms.
  Returns a map of [room-id user-id] -> attendance."
  []
  (let [room-ids (db/select-ids Room :date [:>= (timec/to-sql-date
                                                 (time/minus
                                                  (time/today)
                                                  (time/weeks 1)))])]
    (when (seq room-ids)
      (apply merge
             (for [{:keys [users-id rooms-id attendance]}
                   (db/select UsersRooms :rooms-id [:in room-ids])]
               {[rooms-id users-id] attendance})))))

(defn set-attendance!
  "Takes a `room-id`, `user-id`, `master-id` and `attendance`. Sets
  the value of `attendance` for the UsersRooms enty with `user-id` and
  `room-id`."
  [room-id user-id master-id attendance]
  (binding [*master* (db/select-one User :id master-id)]
    (let [id (db/select-one-id UsersRooms
               :rooms-id room-id
               :users-id user-id)]
      (db/update! UsersRooms id {:attendance attendance}))))

(defn join-room!
  "Create a UsersRooms record that associates a user and a room."
  [room-id master-id]
  (db/insert! UsersRooms
    {:rooms-id   room-id
     :users-id   master-id
     :attendance 0}))

(defn leave-room!
  "Deletes a UsersRooms record that associates a user and a room."
  [room-id master-id]
  (db/delete! UsersRooms
              :rooms-id room-id
              :users-id master-id))

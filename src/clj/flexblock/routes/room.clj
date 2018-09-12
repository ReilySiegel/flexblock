(ns flexblock.routes.room
  (:require [buddy.auth :refer [authenticated?]]
            [clojure.spec.alpha :as spec]
            [flexblock.db :as db]
            [flexblock.rooms :as rooms]
            [flexblock.routes.helpers :refer [api-try]]
            [phrase.alpha :as phrase]
            [ring.util.http-response :as response]))

(defn get-rooms [request]
  (if (authenticated? request)
    (response/ok (db/get-rooms))
    (assoc (response/unauthorized)
           :status 401)))

(defn post-rooms [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (let [{:keys [title description date time room-number max-capacity]
           :as   room} (get-in request [:params])]
      (if-let [error (phrase/phrase-first {} ::rooms/room room)]
        (response/unprocessable-entity {:message error})
        (try (db/insert-room! room (get-in request [:identity :id]))
             (response/ok)
             (catch Exception e
               (response/unprocessable-entity (ex-data e))))))))

(defn join-rooms [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (let [{:keys [room-id]} (:params request)]
      (try (db/join-room! room-id (get-in request [:identity :id]))
           (response/ok)
           (catch Exception e
             (response/unprocessable-entity (ex-data e)))))))

(defn leave-rooms [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (let [{:keys [room-id]} (:params request)]
      (try (db/leave-room! room-id (get-in request [:identity :id]))
           (response/ok)
           (catch Exception e
             (response/unprocessable-entity (ex-data e)))))))

(defn delete-rooms [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (let [{:keys [room-id]} (:params request)]
      (try (db/delete-room! room-id (get-in request [:identity :id]))
           (response/ok)
           (catch Exception e
             (response/unprocessable-entity (ex-data e)))))))

(defn set-attendance [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (let [room-id    (get-in request [:params :room-id])
          user-id    (get-in request [:params :user-id])
          setter-id  (get-in request [:identity :id])
          attendance (get-in request [:params :attendance])]
      (try
        (db/set-attendance! room-id user-id setter-id attendance)
        (response/ok)
        (catch Exception e
          (response/unprocessable-entity (ex-data e)))))))

(defn get-attendance [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (api-try
     (response/ok (db/get-attendance)))))

(defroutes routes
  (GET "/room" []
    :swagger {:summary "Get all Rooms."
              :tags    ["Room"]}
    :return (spec/coll-of ::rooms/room)
    get-rooms)
  (POST "/room" []
    :swagger {:summary    "Create a new Room."
              :tags       ["Room"]
              :description
              "Creates a new room, using the provided auth token to
    discover the creator of the room."
              :parameters {:body ::rooms/room}}
    post-rooms)
  (DELETE "/room" []
    :swagger {:summary    "Delete a room."
              :tags       ["Room"]
              :parameters {:body {:room-id int?}}}
    delete-rooms)
  (POST "/room/join" []
    :swagger {:summary    "Leave a room."
              :tags       ["Room"]
              :parameters {:body {:room-id int?}}}
    join-rooms)
  (POST "/room/leave" []
    :swagger {:summary    "Leave a room."
              :tags       ["Room"]
              :parameters {:body {:room-id int?}}}
    leave-rooms)
  (GET "/room/attendance" []
    :swagger {:summary "Get a map mapping [user-id room-id] -> attendance."
              :tags    ["Room"]}
    get-attendance)
  (POST "/room/attendance" []
    :swagger {:summary    "Set a student's attendance."
              :tags       ["Room"]
              :parameters {:body {:room-id    int?
                                  :user-id    int?
                                  :attendance int?}}}
    set-attendance))

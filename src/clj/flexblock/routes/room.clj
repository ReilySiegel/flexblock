(ns flexblock.routes.room
  (:require [compojure.api.sweet :refer :all :exclude [routes]]
            [clojure.spec.alpha :as spec]
            [ring.util.http-response :as response]
            [flexblock.middleware :as m]
            [buddy.auth :refer [authenticated? throw-unauthorized]]
            [flexblock.db :as db]
            [flexblock.routes.user :as user]
            [flexblock.rooms :as rooms]
            [flexblock.validation]
            [phrase.alpha :as phrase]))

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
        (try (db/insert-room! (get-in request [:identity :id])
                              title
                              description
                              date
                              time
                              room-number
                              max-capacity)
             (response/ok)
             (catch Exception e
               (response/unprocessable-entity (ex-data e))))))))

(defn join-rooms [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (let [{:keys [room-id]} (:params request)]
      (try (db/join-room (get-in request [:identity :id]) room-id)
           (response/ok)
           (catch Exception e
             (response/unprocessable-entity (ex-data e)))))))

(defn leave-rooms [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (let [{:keys [room-id]} (:params request)]
      (try (db/leave-room (get-in request [:identity :id]) room-id)
           (response/ok)
           (catch Exception e
             (response/unprocessable-entity (ex-data e)))))))

(defn delete-rooms [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (let [{:keys [room-id]} (:params request)]
      (try (db/delete-room! (get-in request [:identity :id]) room-id)
           (response/ok)
           (catch Exception e
             (response/unprocessable-entity (ex-data e)))))))

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
    leave-rooms))

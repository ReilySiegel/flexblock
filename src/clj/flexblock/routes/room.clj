(ns flexblock.routes.room
  (:require [compojure.core :refer [defroutes GET POST PATCH DELETE]]
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
    (-> (response/unauthorized)
        (assoc :status 401))))

(defn post-rooms [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (let [{:keys [title description date time room-number max-capacity]
           :as   room} (:params request)]
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
             (response/unprocessable-entity (ex-info e)))))))

(defn delete-rooms [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (let [{:keys [room-id]} (:params request)]
      (try (db/delete-room! (get-in request [:identity :id]) room-id)
           (response/ok)
           (catch Exception e
             (response/unprocessable-entity (ex-info e)))))))

(defroutes routes
  (GET "/room" [] get-rooms)
  (POST "/room" [] post-rooms)
  (DELETE "/room" [] delete-rooms)
  (POST "/room/join" [] join-rooms)
  (POST "/room/leave" [] leave-rooms))

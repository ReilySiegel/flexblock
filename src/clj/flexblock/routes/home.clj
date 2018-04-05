(ns flexblock.routes.home
  (:require [flexblock.layout :as layout]
            [compojure.core :refer [defroutes GET POST DELETE]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [clj-time.core :as time]
            [flexblock.middleware :as m]
            [buddy.auth :refer [authenticated? throw-unauthorized]]
            [flexblock.db :as db]))

(defn home-page [request]
  (layout/render "home.html"))

(defn login [request] 
  (if-let [{username :username password :password } (:params request)]
    (if-let [user (db/check-login username password)]
      (response/ok {:token (m/token user)
                    :user  user})
      (response/bad-request {:message "Login Failed"}))
    (response/bad-request "Invalid Request")))

(defn get-rooms [request] 
  (if (authenticated? request)
    (response/ok (db/get-rooms))
    (-> (response/unauthorized)
        (assoc :status 401))))

(defn get-users [request] 
  (if (authenticated? request)
    (response/ok (db/get-users))
    (-> (response/unauthorized)
        (assoc :status 401))))

(defn post-rooms [request] 
  (if (authenticated? request)
    (if-let [{:keys [title description date time room-number max-capacity]} (:params request)] 
      (let [insert (db/insert-room! (get-in request [:identity :id])
                                    title
                                    description
                                    date
                                    time
                                    room-number
                                    max-capacity)] 
        (if (string? insert)
          (response/internal-server-error {:message insert})
          (response/ok)))
      (response/internal-server-error {:message "Invalid Request"}))
    (response/unauthorized)))

(defn join-rooms [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (let [{:keys [room-id]} (:params request)
          join              (db/join-room (get-in request [:identity :id]) room-id)]
      (if (string? join)
        (response/internal-server-error {:message join})
        (response/ok)))))

(defn leave-rooms [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (let [{:keys [room-id]} (:params request)
          leave             (db/leave-room (get-in request [:identity :id]) room-id)]
      (if (string? leave)
        (response/internal-server-error {:message leave})
        (response/ok)))))

(defn delete-rooms [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (let [{:keys [room-id]} (:params request)
          delete            (db/delete-room! (get-in request [:identity :id]) room-id)]
      (if (string? delete)
        (response/internal-server-error {:message delete})
        (response/ok)))))

(defroutes home-routes
  (GET "/" [] home-page)
  (POST "/login" [] login)
  (GET "/rooms" [] get-rooms)
  (GET "/users" [] get-users)
  (POST "/rooms" [] post-rooms)
  (DELETE "/rooms" [] delete-rooms)
  (POST "/rooms/join" [] join-rooms)
  (POST "/rooms/leave" [] leave-rooms))

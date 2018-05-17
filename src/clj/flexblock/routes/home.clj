(ns flexblock.routes.home
  (:require [flexblock.layout :as layout]
            [compojure.core :refer [defroutes GET POST PATCH DELETE]]
            [ring.util.http-response :as response]
            [flexblock.middleware :as m]
            [buddy.auth :refer [authenticated? throw-unauthorized]]
            [flexblock.db :as db]
            [flexblock.routes.user :as user]
            [flexblock.rooms :as rooms]
            [flexblock.validation]
            [phrase.alpha :as phrase]))

(defn home-page [request]
  (layout/render "home.html"))

(defn login [request]
  (if-let [{username :username password :password } (:params request)]
    (if-let [user (db/check-login username password)]
      (response/ok {:token (m/token user)
                    :user  user})
      (response/bad-request {:message "Login Failed"}))
    (response/bad-request "Invalid Request")))

(defroutes home-routes
  (GET "/" [] home-page)
  (POST "/login" [] login))

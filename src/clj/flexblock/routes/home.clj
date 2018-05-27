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

(defroutes home-routes
  (GET "/" [] home-page))

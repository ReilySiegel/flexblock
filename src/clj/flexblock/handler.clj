(ns flexblock.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [flexblock.layout :refer [error-page]]
            [flexblock.routes.home :refer [home-routes]]
            [flexblock.routes.user :as user]
            [flexblock.routes.room :as room]
            [compojure.route :as route]
            [flexblock.env :refer [defaults]]
            [mount.core :as mount]
            [flexblock.middleware :as middleware]))

(mount/defstate init-app
  :start ((or (:init defaults) identity))
  :stop  ((or (:stop defaults) identity)))

(mount/defstate app
  :start
  (middleware/wrap-base
   (routes
    (-> #'home-routes
        (wrap-routes middleware/wrap-csrf)
        (wrap-routes middleware/wrap-formats))
    (-> #'user/routes
        (wrap-routes middleware/wrap-csrf)
        (wrap-routes middleware/wrap-formats))
    (-> #'room/routes
        (wrap-routes middleware/wrap-csrf)
        (wrap-routes middleware/wrap-formats))
    (route/not-found
     (:body
      (error-page {:status 404
                   :title  "page not found"}))))))

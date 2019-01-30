(ns flexblock.handler
  (:require [flexblock.env :refer [defaults]]
            [flexblock.layout :refer [error-page] :as layout]
            [flexblock.middleware :as middleware]
            [flexblock.routes.rooms :as routes.rooms]
            [flexblock.routes.users :as routes.users]
            [flexblock.users :as users]
            [flexblock.views.home :refer [home]]
            [mount.core :as mount]
            [muuntaja.core :as m]
            reitit.coercion.spec
            [reitit.ring :as ring]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [ring.middleware.gzip :as gzip]))

(mount/defstate init-app
  :start ((or (:init defaults) identity))
  :stop  ((or (:stop defaults) identity)))

(def router
  (ring/router
   [["/" {:handler (fn [_] (layout/render (home)))
          :no-doc  true}]
    ["/rooms" {:restricted? true
               :swagger     {:tags ["Rooms"]}}
     routes.rooms/routes]
    ["/users" {:restricted? true
               :swagger     {:tags ["Users"]}}
     routes.users/routes]
    ["/swagger.json"
     {:get {:no-doc  true
            :swagger
            {:info     {:title "Flexblock API"}
             :securityDefinitions
             {:jwt {:type :apiKey
                    :in   :header
                    :name "Authorization"
                    :description
                    "When authenticating with a token, the token must
      be prepended with the string \"Token \", as per JWT
      standard. For example, assuming a token of \"my_token\", the
      Authorization header should be set to \"Token my_token\"."}}
             :security [{:jwt []}]}
            :handler (swagger/create-swagger-handler)}}]]
   {:conflicts nil
    :data
    {:coercion   reitit.coercion.spec/coercion
     :muuntaja   m/instance
     :middleware middleware/middleware}}))

(mount/defstate app
  :start
  (ring/ring-handler
   router
   (ring/routes
    (swagger-ui/create-swagger-ui-handler
     {:path   "/api"
      :config {:validatorUrl nil}})
    (gzip/wrap-gzip
     (ring/create-resource-handler {:path "/"}))
    (ring/create-default-handler
     {:not-found (constantly
                  (error-page
                   {:status 404
                    :title  "How did you end up here?"}))}))))

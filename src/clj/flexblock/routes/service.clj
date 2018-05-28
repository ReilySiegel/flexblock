(ns flexblock.routes.service
  (:require
   [compojure.core :refer [wrap-routes]]
   [compojure.api.sweet :refer :all]
   [compojure.api.coercion.spec :as spec-coercion]
   [flexblock.middleware :as middleware]
   [flexblock.routes.user :as user]
   [flexblock.routes.room :as room]))

(defapi flexblock-api
  :coercion :spec
  :swagger
  {:ui   "/api-docs"
   :spec "/swagger.json"}
  user/routes
  room/routes)

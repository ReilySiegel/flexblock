(ns flexblock.routes.service
  (:require
   [compojure.core :refer [wrap-routes]]
   [compojure.api.sweet :refer :all]
   [compojure.api.coercion.spec :as spec-coercion]
   [flexblock.middleware :as middleware]
   [flexblock.routes.user :as user]
   [flexblock.routes.room :as room]))


(def no-response-coercion
  (spec-coercion/create-coercion
   spec-coercion/default-coercion))


(swagger-routes)
(defapi flexblock-api
  :coercion :spec
  :swagger
  {:ui   "/api-docs"
   :spec "/swagger.json"}
  user/routes
  room/routes)

(let [{{x :x
        y :y} :query-params} {:query-params {:x 1 :y 2}}]
  [x y])

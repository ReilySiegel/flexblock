(ns flexblock.middleware
  (:require [buddy.auth :refer [authenticated?]]
            [buddy.auth.backends.token :refer [jwe-backend]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.core.nonce :refer [random-bytes]]
            [buddy.sign.jwt :refer [encrypt]]
            [clj-time.core :as time]
            [flexblock.config :refer [env]]
            [flexblock.models.helpers :as models.helpers]
            [flexblock.views.home :refer [*app-context*]]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [ring.middleware.gzip :as gzip]
            [flexblock.db :as db])
  (:import javax.servlet.ServletContext))

(defonce secret (random-bytes 32))

(def token-backend
  (jwe-backend {:secret secret}))

(defn token [user]
  (let [claims (assoc user :exp (time/plus (time/now) (time/days 1)))]
    (encrypt claims secret)))

(defn auth [handler]
  (let [backend token-backend]
    (-> handler
        (wrap-authentication backend)
        (wrap-authorization backend))))

(defn restrict-middleware [handler]
  (fn [request]
    (let [restricted? (some-> request ring/get-match :data :restricted?)]
      (if (and restricted?
               (not (authenticated? request)))
        {:status 401}
        (handler request)))))

(defn wrap-context [handler]
  (fn [request]
    (binding [*app-context*
              (if-let [context (:servlet-context request)]
                ;; If we're not inside a servlet environment
                ;; (for example when using mock requests), then
                ;; .getContextPath might not exist
                (try (.getContextPath ^ServletContext context)
                     (catch IllegalArgumentException _ context))
                ;; if the context is not specifievd in the request
                ;; we check if one has been specified in the environment
                ;; instead
                (:app-context env))]
      (handler request))))

(defn exception-handler [exception request]
  {:status 500
   :body   (select-keys (ex-data exception)
                        [:message])})

(def exception-middleware
  (exception/create-exception-middleware
   (merge exception/default-handlers
          {:domain exception-handler})))

(defn master-middleware
  "Sets the value of the dynamic var `flexblock.models.helpers/*master*`
  to the user who made the request, if they are logged in."
  [handler]
  (fn [{{:keys [id]} :identity
        :as          request}]
    (binding [models.helpers/*master*
              ;; Guard against NPE.
              (when id (db/get-user id))]
      (handler request))))

(def middleware
  [wrap-context
   ;; query-params & form-params
   parameters/parameters-middleware
   ;; content-negotiation
   muuntaja/format-negotiate-middleware
   ;; encoding response body
   muuntaja/format-response-middleware
   ;; exception handling
   exception-middleware
   ;; decoding request body
   muuntaja/format-request-middleware
   ;; coercing response bodys
   #_ coercion/coerce-response-middleware
   ;; coercing request parameters
   coercion/coerce-request-middleware
   ;; multipart
   multipart/multipart-middleware
   ;; auth
   auth
   restrict-middleware
   ;; Set `flexblock.modals.helpers/*master*`
   master-middleware
   ;; gzip
   gzip/wrap-gzip])

(ns flexblock.routes.user
  (:require [buddy.auth :refer [authenticated?]]
            [clj-time.coerce :as timec]
            [clojure.core.async :as async]
            [clojure.spec.alpha :as s]
            [compojure.api.sweet :refer :all :exclude [routes]]
            [flexblock.db :as db]
            [flexblock.middleware :as m]
            [flexblock.notifier.core :as notifier]
            [flexblock.routes.helpers :refer [api-try]]
            [flexblock.users :as users]
            [phrase.alpha :as phrase]
            [ring.util.http-response :as response]))

(defn get-users [request]
  (if (authenticated? request)
    (response/ok (db/get-users))
    (assoc (response/unauthorized)
           :status 401)))

(defn update-password [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (api-try (db/set-password!
              (get-in request [:params :user-id])
              (get-in request [:identity :id])
              (get-in request [:params :password]))
             (response/ok))))

(defn flexblock-reminder [request]
  (if-not (and (authenticated? request)
               (get-in request [:identity :admin]))
    (response/unauthorized)
    (let [date  (get-in request [:params :date])
          ids   (get-in request [:params :user-ids])
          users (db/get-users ids)]
      (doseq [user users]
        (async/put! notifier/notifier
                    {:event     :user/unenrolled
                     :recipient user
                     :date      date}))
      (response/ok))))

(defn post-users [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (api-try
     (db/insert-user! (:params request)
                      (get-in request [:identity :id]))
     (response/ok))))

(defn login [request]
  (if-let [user (db/check-login (get-in request [:params :username])
                                (get-in request [:params :password]))]
    (response/ok {:token (m/token user)
                  :user  user})
    (response/bad-request {:message "Login Failed"})))

(defn delete-user [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (api-try
     (db/delete-user! (get-in request [:params :user-id])
                      (get-in request [:identity :id]))
     (response/ok))))

(defroutes routes
  (GET "/user" []
    :swagger {:summary "Get all users."
              ;; compojure-api does not currently handle multi-specs
              ;; :return  (spec/coll-of ::users/user)
              :tags    ["User"]
              :description
              "Returns a vector of
    `:flexblock.users/user`. compojure-api does not currently support
    multi-specs, so please see `:flexblock.users/user` spec for
    details."}
    get-users)
  (POST "/user" []
    :swagger {:summary     "Add a new user."
              :tags        ["User"]
              ;; compojure-api does not currently handle multi-specs
              ;; :parameters {:body ::users/user}
              :description "Takes a `:flexblock.users/user`as a body
    parameter. compojure-api does not currently support multi-specs,
    so please see `:flexblock.users/user` spec for details."}
    post-users)
  (DELETE "/user" []
    :swagger {:summary     "Remove a user."
              :tags        ["User"]
              ;; compojure-api does not currently handle multi-specs
              :parameters  {:body (s/keys :req-un [::users/id])}
              :description "Takes a user-id as a body parameter."}
    delete-user)
  (POST "/user/flexblock" []
    :swagger {:summary     "Send FlexBlock reminder."
              :tags        ["User"]
              :parameters  {:body {:date     inst?
                                   :user-ids (s/coll-of ::users/id)}}
              :description "Sends a reminder to all students and
    teachers (excludes administrators who are not also teachers) that
    have not creates a FlexBlock session on `date`."}
    flexblock-reminder)
  (POST "/login" []
    :swagger {:summary     "Get a token for a user."
              :tags        ["User"]
              :parameters  {:body {:username ::users/email
                                   :password ::users/password}}
              :description "Get a token for a user."} login)
  (PATCH "/user/password" []
    :swagger {:summary    "Update password."
              :tags       ["User"]
              :parameters {:body {:user-id  int?
                                  :password ::users/password}}}
    update-password))

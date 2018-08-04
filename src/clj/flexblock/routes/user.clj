(ns flexblock.routes.user
  (:require
   [compojure.api.sweet :refer :all :exclude [routes]]
   [ring.util.http-response :as response]
   [buddy.auth :refer [authenticated? throw-unauthorized]]
   [flexblock.db :as db]
   [flexblock.users :as users]
   [clojure.core.async :as async]
   [flexblock.notifier.core :as notifier]
   [flexblock.validation]
   [phrase.alpha :as phrase]
   [flexblock.middleware :as m]
   [clojure.spec.alpha :as spec]
   [clojure.spec.alpha :as s]))

(defn get-users [request]
  (if (authenticated? request)
    (response/ok (db/get-users))
    (assoc (response/unauthorized)
           :status 401)))

(defn update-password [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (let [{:keys [user-id password]} (:params request)]
      (try (db/set-password
            password
            user-id
            (get-in request [:identity :id]))
           (response/ok)
           (catch Exception e
             (response/internal-server-error (ex-data e)))))))

(defn flexblock-date-mailer [request]
  (if-not (and (authenticated? request)
               (get-in request [:identity :admin]))
    (response/unauthorized)
    (let [users    (db/get-users)
          date     (get-in request [:params :date])
          students (->> users
                        (remove :admin)
                        (remove #(users/flexblock-on-date? % date)))]
      (doseq [student students]
        (async/put! notifier/notifier
                    {:event     :user/unenrolled
                     :recipient student
                     :date      date}))
      (response/ok))))

(defn post-users [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (let [{:keys [name email class password teacher admin advisor-id]
           :as   user
           :or   {teacher  false
                  admin    false
                  password (users/gen-password 16)}}
          (merge (:params request)
                 {:advisor-id (get-in request [:identity :id])})]
      (if-let [error (phrase/phrase-first {} ::users/user user)]
        (response/unprocessable-entity {:message error})
        (try
          (db/insert-user! (get-in request [:identity :id])
                           email
                           password
                           name
                           teacher
                           admin
                           class
                           advisor-id)
          (response/ok)
          (catch Exception e
            (response/unprocessable-entity
             (or
              ;; Application Exception
              (ex-data e)
              ;; SQL Exception: user already exists
              {:message "User already exists."}))))))))

(defn login [request]
  (if-let [{username :username password :password } (:params request)]
    (if-let [user (db/check-login username password)]
      (response/ok {:token (m/token user)
                    :user  user})
      (response/bad-request {:message "Login Failed"}))
    (response/bad-request "Invalid Request")))

(defn delete-user [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (try
      (db/delete-user! (get-in request [:params :user-id])
                       (get-in request [:identity :id]))
      (response/ok)
      (catch Exception e
        (response/unprocessable-entity (ex-data e))))))

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
              :parameters  {:body {:date inst?}}
              :description "Sends a reminder to all students and
    teachers (excludes administrators who are not also teachers) that
    have not creates a FlexBlock session on `date`."}
    flexblock-date-mailer)
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

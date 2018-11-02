(ns flexblock.routes.users
  (:require [clojure.core.async :as async]
            [clojure.spec.alpha :as s]
            [flexblock.db :as db]
            [flexblock.middleware :as middleware]
            [flexblock.notifier.core :as notifier]
            [flexblock.users :as users]
            [ring.util.http-response :as response]))

(defn login [{{:keys [username password]} :body-params}]
  (if-let [user (db/check-login username password)]
    {:status 200
     :body   {:token (middleware/token user)
              :user  user}}
    (response/bad-request {:message "Login Failed"})))

(defn flexblock-reminder [{{{:keys [date ids]} :body} :parameters
                           {:keys [admin]}            :identity}]
  (if-not admin
    (response/unauthorized)
    (do (doseq [user (db/get-users ids)]
          (async/put! notifier/notifier
                      {:event     :user/unenrolled
                       :recipient user
                       :date      date}))
        (response/ok))))

(def routes
  [[""
    {:get  {:summary   "Returns a vector of hydrated Users."
            :responses {200 {:body (s/coll-of ::users/user-hydrated)}}
            :handler   (fn [_]
                         (response/ok (vec (db/get-users))))}
     :post {:summary "Add a new user."
            :handler (fn [{user :body-params}]
                       (db/insert-user! user)
                       (response/ok))}}]
   ["/flexblock"
    {:parameters {:body {:date inst?
                         :ids  (s/coll-of ::users/id)}}
     :post       {:summary     "Send flexblock reminder."
                  :description "Sends a reminder to all users in
    user-ids that have not creates a FlexBlock session on `date`."
                  :handler     flexblock-reminder}}]
   ["/login"
    {:parameters  {:body {:username ::users/email
                          :password ::users/password}}
     :restricted? false
     :post
     {:summary     "Get a login token."
      :description "When authenticating with a token, the token must
      be prepended with the string \"Token \", as per JWT
      standard. For example, assuming a token of \"my_token\", the
      Authorization header should be set to \"Token my_token\"."
      :swagger     {:tags     ["Login"]
                    :security []}
      :handler     login}}]
   ["/:id"
    {:parameters {:path {:id ::users/id}}
     :delete     {:summary "Delete a user."
                  :handler
                  (fn [{{{:keys [id]} :path} :parameters}]
                    (db/delete-user! id)
                    (response/ok))}}]
   ;; Define a second "/:id", so that the delete handler is not cloned
   ;; to children.
   ["/:id"
    {:parameters {:path {:id ::users/id}}}
    ["/password"
     {:parameters {:body {:password ::users/password}}
      :put        {:summary "Update password."
                   :handler (fn [{{{:keys [id]}       :path
                                   {:keys [password]} :body} :parameters}]
                              (db/set-password! id password)
                              (response/ok))}}]
    ["/advisor-id"
     {:parameters {:body {:advisor-id ::users/advisor-id}}
      :put        {:summary "Update advisor-id."
                   :handler (fn [{{{:keys [id]}         :path
                                   {:keys [advisor-id]} :body} :parameters}]
                              (db/set-advisor-id! id advisor-id))}}]]])

(ns flexblock.routes.user
  (:require
   [compojure.core :refer [defroutes context GET POST PATCH DELETE]]
   [ring.util.http-response :as response]
   [buddy.auth :refer [authenticated? throw-unauthorized]]
   [flexblock.db :as db]
   [flexblock.users :as users]
   [clojure.core.async :as async]
   [flexblock.notifier :as notifier]
   [flexblock.validation]
   [phrase.alpha :as phrase]))

(defn get-users [request]
  (if (authenticated? request)
    (response/ok (db/get-users))
    (-> (response/unauthorized)
        (assoc :status 401))))

(defn update-password [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (let [{:keys [user-id password]} (:params request)
          delete                     (db/set-password
                                      password
                                      user-id
                                      (get-in request [:identity :id]))]
      (if (string? delete)
        (response/internal-server-error {:message delete})
        (response/ok)))))

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
           :or   {teacher? false
                  admin?   false
                  password (users/gen-password 16)}}
          (merge (:params request)
                 {:advisor-id (get-in request [:identity :id])})]
      (if-let [error (phrase/phrase-first {} ::users/user user)]
        (response/unprocessable-entity {:message error})
        (let [result (try
                       (db/insert-user! (get-in request [:identity :id])
                                        email
                                        password
                                        name
                                        teacher
                                        admin
                                        class
                                        advisor-id)
                       (catch Throwable t
                         (:cause (Throwable->map t))))]
          (if (string? result)
            (response/unprocessable-entity {:message result})
            (response/ok)))))))

(defroutes routes
  (GET "/user" [] get-users)
  (POST "/user" [] post-users)
  (POST "/user/flexblock" [] flexblock-date-mailer)
  (PATCH "/user/password" [] update-password))

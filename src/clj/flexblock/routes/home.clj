(ns flexblock.routes.home
  (:require [flexblock.layout :as layout]
            [compojure.core :refer [defroutes GET POST PATCH DELETE]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [clj-time.core :as time]
            [flexblock.middleware :as m]
            [buddy.auth :refer [authenticated? throw-unauthorized]]
            [flexblock.db :as db]
            [flexblock.users :as users]
            [flexblock.rooms :as rooms]
            [clojure.core.async :as async]
            [flexblock.notifier :as notifier]
            [flexblock.validation]
            [phrase.alpha :as phrase]))

(defn home-page [request]
  (layout/render "home.html"))

(defn login [request]
  (if-let [{username :username password :password } (:params request)]
    (if-let [user (db/check-login username password)]
      (response/ok {:token (m/token user)
                    :user  user})
      (response/bad-request {:message "Login Failed"}))
    (response/bad-request "Invalid Request")))

(defn get-rooms [request]
  (if (authenticated? request)
    (response/ok (db/get-rooms))
    (-> (response/unauthorized)
        (assoc :status 401))))

(defn get-users [request]
  (if (authenticated? request)
    (response/ok (db/get-users))
    (-> (response/unauthorized)
        (assoc :status 401))))

(defn post-rooms [request]
  (if (authenticated? request)
    (if-let [{:keys [title description date time room-number max-capacity]
              :as   room} (:params request)]
      (if-let [error (phrase/phrase-first {} ::rooms/room room)]
        (response/unprocessable-entity {:message error})
        (let [insert (db/insert-room! (get-in request [:identity :id])
                                      title
                                      description
                                      date
                                      time
                                      room-number
                                      max-capacity)]
          (if (string? insert)
            (response/internal-server-error {:message insert})
            (response/ok))))
      (response/internal-server-error {:message "Invalid Request"}))
    (response/unauthorized)))

(defn join-rooms [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (let [{:keys [room-id]} (:params request)
          join              (db/join-room (get-in request [:identity :id]) room-id)]
      (if (string? join)
        (response/internal-server-error {:message join})
        (response/ok)))))

(defn leave-rooms [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (let [{:keys [room-id]} (:params request)
          leave             (db/leave-room
                             (get-in request [:identity :id]) room-id)]
      (if (string? leave)
        (response/internal-server-error {:message leave})
        (response/ok)))))

(defn delete-rooms [request]
  (if-not (authenticated? request)
    (response/unauthorized)
    (let [{:keys [room-id]} (:params request)
          delete            (db/delete-room!
                             (get-in request [:identity :id]) room-id)]
      (if (string? delete)
        (response/internal-server-error {:message delete})
        (response/ok)))))

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
                        (remove :teacher)
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
    (let [{:keys [name email class password teacher? admin?]
           :or   {teacher? false
                  admin?   false
                  password (apply str
                                  (take 12
                                        (repeatedly
                                         #(char
                                           (+ (rand 26) (rand-nth
                                                         [97 65]))))))}}
          (:params request)
          result (try (db/insert-user! (get-in request [:identity :id])
                                       email
                                       password
                                       name
                                       teacher?
                                       admin?
                                       class
                                       (get-in request [:identity :id]))
                      (catch Throwable t
                        (:cause (Throwable->map t))))]
      (if (string? result)
        (response/unprocessable-entity {:message result})
        (response/ok)))))

(defroutes home-routes
  (GET "/" [] home-page)
  (POST "/login" [] login)
  (GET "/rooms" [] get-rooms)
  (GET "/users" [] get-users)
  (POST "/users" [] post-users)
  (POST "/rooms" [] post-rooms)
  (DELETE "/rooms" [] delete-rooms)
  (POST "/rooms/join" [] join-rooms)
  (POST "/rooms/leave" [] leave-rooms)
  (PATCH "/user/password" [] update-password)
  (POST "/user/flexblock" [] flexblock-date-mailer))

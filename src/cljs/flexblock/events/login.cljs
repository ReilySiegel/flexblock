(ns flexblock.events.login
  (:require [re-frame.core :as rf]
            [flexblock.db :as db]
            [ajax.core :as ajax]))

(rf/reg-event-fx
 :login/set-user
 (fn [{:keys [db]} [_ {:keys [user token]}]]
   {:db           (-> db
                      (assoc :token token)
                      (assoc :user user))
    :notification "Logged In"
    :localstorage {:token token
                   :user  user}
    :close-modal  "#login-modal"}))

(rf/reg-event-fx
 :logout
 (fn [_ _]
   {:db                     db/default-db
    :overwrite-localstorage {}
    :notification           "Logged Out"}))

(rf/reg-event-fx
 :login
 (fn [{:keys [db]} [_ username password]]
   {:http-xhrio {:method          :post
                 :uri             "/login"
                 :headers         {"Authorization" (str "Token "
                                                        (:token db))}
                 :params          {:username username :password password}
                 :format          (ajax/json-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:login/set-user]
                 :on-failure      [:http/failure]}}))

(ns flexblock.login.events
  (:require [ajax.core :as ajax]
            [re-frame.core :as rf]))

(rf/reg-event-fx
 :login/set-user
 (fn [{:keys [db]} [_ {:keys [user token]}]]
   {:db           (-> db
                      (assoc :login/token token)
                      (assoc :login/user user))
    :notification "Logged In"
    :dispatch-n   [[:rooms/get]
                   [:users/get]
                   [:login/set-open false]]
    :localstorage {:login/token token
                   :login/user  user}}))

(rf/reg-event-fx
 :logout
 (fn [{:keys [db]} _]
   {:localstorage   {:login/token ""
                     :login/user  {}}
    :notification   "Logged Out"
    ;; Wait for localstorage to be overwritten.
    :dispatch-later [{:ms 250 :dispatch [:initialize-db]}]}))

(rf/reg-event-fx
 :login
 (fn [{:keys [db]} [_ username password]]
   {:http-xhrio {:method          :post
                 :uri             "/users/login"
                 :params          {:username username :password password}
                 :format          (ajax/json-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:login/set-user]
                 :on-failure      [:http/failure]}}))

(rf/reg-event-db
 :login/set-open
 (fn [db [_ open?]]
   (assoc db :login/open open?)))

(ns flexblock.users.events
  (:require [re-frame.core :as rf]
            [ajax.core :as ajax]
            [flexblock.db :as db]
            [goog.string :as gstring]
            [goog.string.format]))

(rf/reg-event-fx
 :users/set
 (fn [{:keys [db]} [_ users]]
   {:db (assoc db :users users)}))

(rf/reg-event-fx
 :users/get
 (fn [{:keys [db]} _]
   {:http-xhrio {:method          :get
                 :uri             "/users"
                 :format          (ajax/transit-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:users/set]
                 :on-failure      [:http/failure]}}))


(rf/reg-event-fx
 :users/reset-password-success
 (fn [_ [_ response]]
   {:notification "Password reset."
    :close-modal  "#password-modal"}))

(rf/reg-event-fx
 :users/reset-password
 (fn [{:keys [db]} [_ user-id]]
   {:http-xhrio {:method          :put
                 :uri             (gstring/format "/users/%s/password"
                                                  user-id)
                 :params          {:password (:password db)}
                 :format          (ajax/json-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:users/reset-password-success]
                 :on-failure      [:http/failure]}}))

(rf/reg-event-fx
 :users/post-user-success
 (fn [_ [_ response]]
   {:notification "User added."
    :dispatch     [:users/get]
    :close-modal  "#add-user-modal"}))

(rf/reg-event-fx
 :users/post-user
 (fn [{:keys [db]} [_ user]]
   {:http-xhrio {:method          :post
                 :uri             "/users"
                 :params          user
                 :format          (ajax/json-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:users/post-user-success]
                 :on-failure      [:http/failure]}}))

(rf/reg-event-fx
 :users/delete-success
 (fn [_ [_ response]]
   {:notification "User removed."
    :dispatch     [:users/get]}))

(rf/reg-event-fx
 :users/delete
 (fn [{:keys [db]} [_ user-id]]
   {:http-xhrio {:method          :delete
                 :uri             (str "/users/" user-id)
                 :format          (ajax/json-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:users/delete-success]
                 :on-failure      [:http/failure]}}))

(rf/reg-event-fx
 :users/claim-success
 (fn [_ [_ response]]
   {:notification "User claimed."
    :dispatch     [:users/get]}))

(rf/reg-event-fx
 :users/claim
 (fn [{:keys [db]} [_ user-id]]
   {:http-xhrio {:method          :put
                 :uri             (gstring/format "/users/%s/advisor-id"
                                                  user-id)
                 :params          {:advisor-id (get-in db [:login/user :id])}
                 :format          (ajax/json-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:users/claim-success]
                 :on-failure      [:http/failure]}}))

(rf/reg-event-fx
 :users/abandon-success
 (fn [_ [_ response]]
   {:notification "User abandoned."
    :dispatch     [:users/get]}))

(rf/reg-event-fx
 :users/abandon
 (fn [{:keys [db]} [_ user-id]]
   {:http-xhrio {:method          :put
                 :uri             (gstring/format "/users/%s/advisor-id"
                                                  user-id)
                 :params          {:advisor-id (get-in db [:user :id])}
                 :format          (ajax/json-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:users/abandon-success]
                 :on-failure      [:http/failure]}}))


(rf/reg-event-fx
 :users/set-session-modal
 (fn [{:keys [db]} [_ user]]
   {:db         (assoc db :session-modal user)
    :open-modal "#session-modal"}))

(rf/reg-event-fx
 :users/set-password-modal
 (fn [{:keys [db]} [_ user]]
   {:db         (assoc db :password-modal user)
    :open-modal "#password-modal"}))

(rf/reg-event-fx
 :users/set-password
 (fn [{:keys [db]} [_ password]]
   {:db (assoc db :password password)}))

(rf/reg-event-db
 :users/toggle-filter
 (fn [db _]
   (update db :users/filter not)))

(rf/reg-event-db
 :users/update-role-filter
 (fn [db [_ key filter?]]
   (if filter?
     (update db :users/role-filter conj key)
     (update db :users/role-filter disj key))))

(ns flexblock.events.students
  (:require [re-frame.core :as rf]
            [ajax.core :as ajax]
            [flexblock.db :as db]))

(rf/reg-event-fx
 :user/set
 (fn [{:keys [db]} [_ rooms]]
   {:db (assoc db :users rooms)}))

(rf/reg-event-fx
 :user/get
 (fn [{:keys [db]} _]
   {:http-xhrio {:method          :get
                 :uri             "/user"
                 :headers         {"Authorization" (str "Token "
                                                        (:token db))}
                 :format          (ajax/transit-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:user/set]
                 :on-failure      [:http/failure]}}))


(rf/reg-event-fx
 :user/reset-password-success
 (fn [_ [_ response]]
   {:notification "Password reset."}))

(rf/reg-event-fx
 :user/reset-password
 (fn [{:keys [db]} [_ user-id password]]
   {:http-xhrio {:method          :patch
                 :uri             "/user/password"
                 :headers         {"Authorization" (str "Token "
                                                        (:token db))}
                 :params          {:user-id user-id :password password}
                 :format          (ajax/json-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:user/reset-password-success]
                 :on-failure      [:http/failure]}}))

(rf/reg-event-fx
 :user/post-user-success
 (fn [_ [_ response]]
   {:notification "User added."
    :dispatch     [:user/get]
    :close-modal "#add-user-modal"}))

(rf/reg-event-fx
 :user/post-user
 (fn [{:keys [db]} [_ user]]
   {:http-xhrio {:method          :post
                 :uri             "/user"
                 :headers         {"Authorization" (str "Token "
                                                        (:token db))}
                 :params          user
                 :format          (ajax/json-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:user/post-user-success]
                 :on-failure      [:http/failure]}}))

(rf/reg-event-fx
 :user/delete-success
 (fn [_ [_ response]]
   {:notification "User removed."
    :dispatch     [:user/get]}))

(rf/reg-event-fx
 :user/delete
 (fn [{:keys [db]} [_ user-id]]
   {:http-xhrio {:method          :delete
                 :uri             "/user"
                 :headers         {"Authorization" (str "Token "
                                                        (:token db))}
                 :params          {:user-id user-id}
                 :format          (ajax/json-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:user/delete-success]
                 :on-failure      [:http/failure]}}))

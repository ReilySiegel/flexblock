(ns flexblock.events.students
  (:require [re-frame.core :as rf]
            [ajax.core :as ajax]
            [flexblock.utils :as u]
            [flexblock.db :as db]))

(rf/reg-sub
 :add-user/email
 (fn [db _]
   (get-in db [:add-user :email])))

(rf/reg-sub
 :add-user/name
 (fn [db _]
   (get-in db [:add-user :name])))

(rf/reg-sub
 :add-user/teacher
 (fn [db _]
   (get-in db [:add-user :teacher])))

(rf/reg-sub
 :add-user/admin
 (fn [db _]
   (get-in db [:add-user :admin])))

(rf/reg-sub
 :add-user/class
 (fn [db _]
   (get-in db [:add-user :class])))

(rf/reg-event-db
 :add-user/set-email
 (fn [db [_ email]]
   (assoc-in db [:add-user :email] email)))

(rf/reg-event-db
 :add-user/set-name
 (fn [db [_ name]]
   (assoc-in db [:add-user :name] name)))

(rf/reg-event-db
 :add-user/set-admin
 (fn [db [_ admin]]
   (assoc-in db [:add-user :admin] admin)))

(rf/reg-event-db
 :add-user/set-teacher
 (fn [db [_ teacher]]
   (assoc-in db [:add-user :teacher] teacher)))

(rf/reg-event-db
 :add-user/set-class
 (fn [db [_ class]]
   (assoc-in db [:add-user :class] class)))

(rf/reg-event-db
 :add-user/reset
 (fn [db _]
   (assoc db :add-user db/user-db)))

(rf/reg-event-fx
 :user/post-user-success
 (fn [_ [_ response]]
   (u/get-users)
   {:notification "User added."
    :dispatch     [:add-user/reset]}))

(rf/reg-event-fx
 :user/post-user
 (fn [{:keys [db]}]
   {:http-xhrio {:method          :post
                 :uri             "/users"
                 :headers         {"Authorization" (str "Token "
                                                        (:token db))}
                 :params          (if (get-in db [:user :admin])
                                    (:add-user db)
                                    (merge (:add-user db)
                                           {:advisor-id
                                            (get-in db
                                                    [:user :id])}))
                 :format          (ajax/json-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:user/post-user-success]
                 :on-failure      [:http/failure]}}))

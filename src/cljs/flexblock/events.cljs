(ns flexblock.events
  (:require [flexblock.db :as db]
            [ajax.core :as ajax]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-sub]]))

;;dispatchers

(reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

(reg-event-db
  :set-active-page
  (fn [db [_ page]]
    (assoc db :page page)))

(reg-event-db
 :set-token
 (fn [db [_ token]]
   (assoc db :token token)))

(reg-event-db
 :login/set-username
 (fn [db [_ username]]
   (assoc-in db [:login :username] username)))

(reg-event-db
 :login/set-password
 (fn [db [_ password]]
   (assoc-in db [:login :password] password)))

(reg-event-db
 :set-rooms
 (fn [db [_ rooms]]
   (assoc db :rooms rooms)))


(reg-event-db
 :inc-loading
 (fn [db _]
   (if (> 10 (:loading db))
     (update db :loading inc)
     db)))

(reg-event-db
 :dec-loading
 (fn [db _]
   (if (pos? (:loading db))
     (update db :loading dec)
     db)))

(reg-event-db
 :add-room/set-title
 (fn [db [_ title]]
   (assoc-in db [:add-room :title] title)))

(reg-event-db
 :add-room/set-description
 (fn [db [_ description]]
   (assoc-in db [:add-room :description] description)))

(reg-event-db
 :add-room/set-date
 (fn [db [_ date]]
   (assoc-in db [:add-room :date] date)))

(reg-event-db
 :add-room/set-room-number
 (fn [db [_ room-number]]
   (if-let [num (js/parseInt room-number)]
     (assoc-in db [:add-room :room-number] num)
     db)))

(reg-event-db
 :add-room/set-max-capacity
 (fn [db [_ max-capacity]]
   (if-let [num (js/parseInt max-capacity)]
     (assoc-in db [:add-room :max-capacity] num)
     db)))

(reg-event-db
 :add-room/set-time
 (fn [db [_ time]] 
   (assoc-in db [:add-room :time] time)))

(reg-event-db
 :add-room/reset-room
 (fn [db _]
   (assoc db :add-room db/room-db)))

(reg-event-db
 :set-user
 (fn [db [_ user]]
   (assoc db :user user)))

(reg-event-db
 :set-search
 (fn [db [_ search]]
   (assoc db :search search)))

(reg-event-db
 :set-users
 (fn [db [_ users]]
   (assoc db :users users)))

(reg-event-db
 :set-date
 (fn [db [_ date]]
   (assoc db :date date)))

(reg-event-db
 :set-reset-password
 (fn [db [_ reset-password]]
   (assoc db :reset-password reset-password)))

(reg-event-fx
 :mailer/post-date
 (fn [{:keys [db]} _]
   {:http-xhrio {:method          :post
                 :uri             "/user/flexblock"
                 :headers         {"Authorization" (str "Token " (:token db))}
                 :params          {:date (:date db)}
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})}}))

;;subscriptions

(reg-sub
 :page
 (fn [db _]
   (:page db)))

(reg-sub
 :token 
 (fn [db _]
   (:token db)))

(reg-sub
 :login/username 
 (fn [db _]
   (get-in db [:login :username])))

(reg-sub
 :login/password 
 (fn [db _]
   (get-in db [:login :password])))

(reg-sub
 :rooms
 (fn [db _]
   (:rooms db)))

(reg-sub
 :room/title
 (fn [db _]
   (get-in db  [:add-room :title])))

(reg-sub
 :room/description
 (fn [db _]
   (get-in db [:add-room :description])))

(reg-sub
 :room/date
 (fn [db _]
   (get-in db [:add-room :date])))

(reg-sub
 :room/number
 (fn [db _]
   (get-in db [:add-room :room-number])))

(reg-sub
 :room/max-capacity
 (fn [db _]
   (get-in db [:add-room :max-capacity])))

(reg-sub
 :room/time
 (fn [db _]
   (get-in db [:add-room :time])))

(reg-sub
 :loading
 (fn [db _]
   (:loading db)))

(reg-sub
 :user-id
 (fn [db _]
   (get-in db [:user :id])))

(reg-sub
 :user
 (fn [db _]
   (:user db)))

(reg-sub
 :search
 (fn [db _]
   (:search db)))

(reg-sub
 :users
 (fn [db _]
   (:users db)))

(reg-sub
 :date
 (fn [db _]
   (:date db)))

(reg-sub
 :reset-password
 (fn [db _]
   (:reset-password db)))

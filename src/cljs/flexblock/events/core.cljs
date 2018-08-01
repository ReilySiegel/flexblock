(ns flexblock.events
  (:require [flexblock.db :as db]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-sub]]
            [flexblock.events.http]
            [flexblock.events.login]
            [flexblock.events.mailer]
            [flexblock.events.room]
            [flexblock.events.students]
            [clojure.string :as str]))

;;dispatchers

(reg-event-db
 :initialize-db
 (fn [_ _]
   (db/default-db)))

(reg-event-db
 :set-active-page
 (fn [db [_ page]]
   (assoc db :page page)))

(reg-event-fx
 :set-search
 (fn [{:keys [db]} [_ search]]
   (merge {:db (assoc db :search search)}
          ;; Easter egg referencing RFC-1149.
          (when (= (str/lower-case search) "ipoac")
            {:notification "A pigeon will deliver your packet shortly."}))))

(reg-event-db
 :set-users
 (fn [db [_ users]]
   (assoc db :users users)))

(reg-event-db
 :set-date
 (fn [db [_ date]]
   (assoc db :date date)))

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
 :rooms
 (fn [db _]
   (:rooms db)))

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

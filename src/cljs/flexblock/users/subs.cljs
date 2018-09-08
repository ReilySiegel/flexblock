(ns flexblock.users.subs
  "This namespace contains the subscription handlers relating to students."
  (:require [flexblock.users :as users]
            [re-frame.core :as rf]))

;; Returns all users in the database.

(rf/reg-sub
 :users/all
 (fn [db _]
   (:users db)))


;; Users after all irrelevant entries have been removed.
(rf/reg-sub
 :users/filtered
 ;; Use raw users, the selected date and the logged-in user as signals.
 :<- [:users/all]
 :<- [:login/user]
 :<- [:date]
 (fn [[users logged-in date] _]
   (let [users (if (:admin logged-in)
                 users
                 (->> users
                      (remove :teacher)
                      (remove :admin)))]
     (if (nil? date)
       users
       (->> users
            (remove #(users/flexblock-on-date? % date))
            (remove :admin))))))


(rf/reg-sub
 :users/sorted
 ;; Subscribe to filtered users and search as signals.
 :<- [:users/filtered]
 :<- [:search]

 ;; Sort the users.
 (fn [[users search] _]
   (let [search (users/make-search search)]
     (->> users
          (sort-by :name)
          (sort-by search #(compare %2 %1))))))

(rf/reg-sub
 :user/get-attendance
 :<- [:rooms/all]
 (fn [rooms [_ room-id user-id]]
   (->> rooms
        (filter #(= room-id (:id %)))
        first
        :users
        (filter #(= user-id (:id %)))
        first
        :attendance)))

(rf/reg-sub
 :users/session-modal
 (fn [db _]
   (:session-modal db)))

(rf/reg-sub
 :users/password-modal
 (fn [db _]
   (:password-modal db)))

(rf/reg-sub
 :users/password
 (fn [db _]
   (:password db)))

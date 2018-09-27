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
(defn filter-users [users logged-in date roles]
  (let [users (->> users
                   (filter (partial users/can-edit? logged-in))
                   (filter #(some (set roles) (users/user-roles %))))]
    (if (nil? date)
      users
      (->> users
           (remove #(users/flexblock-on-date? %
                                              (js/Date.
                                               (.setUTCHours date
                                                             0 0 0 0))))))))

(rf/reg-sub
 :users/filtered
 ;; Use raw users, the selected date and the logged-in user as signals.
 :<- [:users/all]
 :<- [:login/user]
 :<- [:date]
 :<- [:users/role-filter]
 (fn [[users logged-in date roles] _]
   (filter-users users logged-in date roles)))


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

(rf/reg-sub
 :users/filter
 (fn [db _]
   (:users/filter db)))

(rf/reg-sub
 :users/role-filter
 (fn [db _]
   (:users/role-filter db)))

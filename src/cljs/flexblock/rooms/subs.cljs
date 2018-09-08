(ns flexblock.rooms.subs
  (:require [flexblock.rooms :as rooms]
            [re-frame.core :as rf]))


(rf/reg-sub
 :rooms/all
 (fn [db _]
   (:rooms db)))

(rf/reg-sub
 :rooms/filter
 (fn [db _]
   (:rooms/filter db)))

(rf/reg-sub
 :rooms/time-filter
 (fn [db _]
   (:rooms/time-filter db)))

(rf/reg-sub
 :rooms/filtered
 :<- [:rooms/all]
 :<- [:rooms/time-filter]
 (fn [[rooms filters]]
   (if (empty? filters)
     rooms
     (->> rooms
          (filter #(filters (keyword (:time %))))))))

(rf/reg-sub
 :rooms/sorted
 :<- [:rooms/filtered]
 :<- [:search]
 :<- [:login/user]
 (fn [[rooms search user]]
   (let [search (rooms/make-search search)]
     (->> rooms
          (sort-by :date)
          ;; Show rooms created by the logged-in user first.
          (sort-by #(not= (:name user)
                          (:name (rooms/get-teacher %))))
          ;; Search gives higher numbers for better matches, so we
          ;; need to sort in descending order.
          (sort-by search #(compare %2 %1))))))

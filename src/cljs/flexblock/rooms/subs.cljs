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
 :<- [:search-debounced]
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

(rf/reg-sub
 :rooms/attendance-modal
 (fn [db _]
   (:attendance-modal db)))

(rf/reg-sub
 :room/get-attendance
 (fn [db [_ room-id user-id]]
   (get-in db [:attendance [room-id user-id]])))

(rf/reg-sub
 :rooms/modal-open
 (fn [db _]
   (:rooms/modal-open db)))

(rf/reg-sub
 :rooms.form/title
 (fn [db _]
   (get-in db [:rooms/form :title])))

(rf/reg-sub
 :rooms.form/number
 (fn [db _]
   (get-in db [:rooms/form :number])))

(rf/reg-sub
 :rooms.form/capacity
 (fn [db _]
   (get-in db [:rooms/form :capacity])))

(rf/reg-sub
 :rooms.form/description
 (fn [db _]
   (get-in db [:rooms/form :description])))

(rf/reg-sub
 :rooms.form/date
 (fn [db _]
   (get-in db [:rooms/form :date])))

(rf/reg-sub
 :rooms.form/time
 (fn [db _]
   (get-in db [:rooms/form :time])))

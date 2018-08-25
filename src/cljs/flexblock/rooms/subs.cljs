(ns flexblock.rooms.subs
  (:require [flexblock.rooms :as rooms]
            [re-frame.core :as rf]))


(rf/reg-sub
 :rooms/all
 (fn [db _]
   (:rooms db)))


(rf/reg-sub
 :rooms/sorted
 :<- [:rooms/all]
 :<- [:search]
 :<- [:login/user]
 (fn [[rooms search user]]
   (let [search (rooms/make-search rooms search)]
     (->> rooms
          (sort-by :date)
          ;; Show rooms created by the logged-in user first.
          (sort-by #(not= (:name user)
                          (:name (rooms/get-teacher %))))
          ;; Search gives higher numbers for better matches, so we
          ;; need to sort in descending order.
          (sort-by search #(compare %2 %1))))))

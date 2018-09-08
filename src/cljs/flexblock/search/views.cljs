(ns flexblock.search.views
  (:require
   [re-com.core :as rc]
   [re-frame.core :as rf]
   [flexblock.components.input :as input]
   [flexblock.reminder.views :as reminder]))

(defn search-bar []
  [:div.row
   [:div.col.l6.s12.offset-l3
    [rc/input-text
     :width "100%"
     :placeholder "Search"
     :on-change   #(rf/dispatch [:set-search-debounce %])
     :model       @(rf/subscribe [:search])
     :change-on-blur? false]]])


(defn date-bar []
  [:div.row
   (if (:admin @(rf/subscribe [:login/user]))
     [:div.input-field
      [:div.col.l4.m8.s6.offset-l3
       [input/datepicker
        {:dispatch-key  :set-date
         :subscribe-key :date}]]
      [:div.col.l2.m4.s6.center [reminder/button]]]
     [:div.col.l4.m8.s12.offset-l4.offset-m2
      [input/datepicker
       {:dispatch-key  :set-date
        :subscribe-key :date}]])])

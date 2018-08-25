(ns flexblock.search.views
  (:require
   [re-frame.core :as rf]
   [flexblock.components.input :as input]
   [flexblock.reminder.views :as reminder]))

(defn search-bar []
  [:div.row
   [:div.col.l6.s12.offset-l3
    [input/text
     {:placeholder   "Search"
      :dispatch-key  :set-search
      :subscribe-key :search
      :on-change?    true}]]])


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

(ns flexblock.components.search
  (:require
   [re-frame.core :as rf]
   [flexblock.components.emailer :as emailer]
   [flexblock.components.input :as input]))

(defn search-bar [] 
  [:div.row
   [:div.col.l6.m12.offset-l3
    [input/text
     {:placeholder   "Search" 
      :dispatch-key  :set-search
      :sunscribe-key :search
      :on-change?    true}]]])


(defn date-bar [] 
  [:div.row
   (if (:admin @(rf/subscribe [:user]))
     [:div.input-field
      [:div.col.l4.m12.offset-l3 
       [input/datepicker
        {:dispatch-key  :set-date
         :subscribe-key :date}]] 
      [:div.col.l1 [emailer/fab]]]
     
     [:div.col.l4.m12.offset-l4
      [input/datepicker
       {:dispatch-key  :set-date
        :subscribe-key :date}]])])

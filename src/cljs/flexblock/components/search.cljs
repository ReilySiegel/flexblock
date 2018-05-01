(ns flexblock.components.search
  (:require [flexblock.components.input :as input]))

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
   [:div.col.l4.m12.offset-l4
    [input/datepicker
     {:dispatch-key  :set-date
      :subscribe-key :date}]]])

(ns flexblock.components.search
  (:require [flexblock.components.input :as input]))

(defn search-bar [] 
  [:div.row
   [:div.col.l6.m12.offset-l3
    [input/input-rf-dispatch
     {:placeholder "Search"}
     "Search"
     :set-search
     :search
     false
     true]]])


(defn date-bar [] 
  [:div.row
   [:div.col.l4.m12.offset-l4
    [input/input-rf-dispatch
     {:placeholder "Date YYYY/MM/DD"}
     "Date"
     :set-date
     :date
     false
     true]]])

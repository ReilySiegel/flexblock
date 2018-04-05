(ns flexblock.pages.rooms
  (:require [flexblock.components.room :as room]))

(defn page
  "The rooms page."
  []  
  [:div
   [room/fab]
   [room/add]
   [room/grid]])

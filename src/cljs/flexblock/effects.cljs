(ns flexblock.effects
  (:require [re-frame.fx :refer [reg-fx]]))

(reg-fx
 :notification
 (fn [message]
   (.toast js/M (clj->js {:html message}))))

(reg-fx
 :close-modal
 (fn [query-selector]
   (if-let [e (.querySelector js/document query-selector)]
     (.close (.getInstance js/M.Modal e)))))

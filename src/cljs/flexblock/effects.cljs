(ns flexblock.effects
  (:require [re-frame.fx :refer [reg-fx]]
            [flexblock.db :as db]))

(reg-fx
 :reload
 (fn reload
   ([] (reload 0))
   ([delay]
    (.setTimeout
     js/window
     #(.reload js/location true)
     delay))))

(reg-fx
 :notification
 (fn [message]
   (.toast js/M (clj->js {:html message}))))

(reg-fx
 :overwrite-localstorage
 (fn [m]
   (db/set-localstorage! m)))

(reg-fx
 :localstorage
 (fn [m]
   (db/set-localstorage! (merge (db/get-localstorage) m))))

(reg-fx
 :close-modal
 (fn [query-selector]
   (if-let [e (.querySelector js/document query-selector)]
     (.close (.getInstance js/M.Modal e)))))

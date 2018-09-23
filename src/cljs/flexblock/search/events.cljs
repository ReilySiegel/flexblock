(ns flexblock.search.events
  (:require [clojure.string :as str]
            [re-frame.core :as rf]))


(rf/reg-event-fx
 :set-search
 (fn [{:keys [db]} [_ search]]
   (merge {:db (assoc db :search search)}
          ;; Easter egg referencing RFC-1149.
          (when (= (str/lower-case search) "ipoac")
            {:notification "A pigeon will deliver your packet shortly."})
          ;; Reference CS:GO
          (when (= search "7355608")
            {:notification
             "The bomb has been defused. COUNTER-TERRORISTS win."}))))


(rf/reg-event-fx
 :set-search-debounce
 (fn [_ [_ search]]
   {:dispatch-debounce
    [{:id      ::set-search-debounce
      :timeout 500
      :action  :dispatch
      :event   [:set-search search]}]}))

(rf/reg-event-db
 :set-date
 (fn [db [_ date]]
   (assoc db :date date)))

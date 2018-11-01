(ns flexblock.search.events
  (:require [clojure.string :as str]
            [re-frame.core :as rf]))

(rf/reg-event-fx
 :set-search-debounced
 (fn [{:keys [db]} [_ search]]
   {:db (assoc db :search-debounced search)}))


(rf/reg-event-fx
 :set-search-debounce
 (fn [{:keys [db]} [_ search]]
   {:db (assoc db :search search)
    :dispatch-debounce
    [{:id      ::set-search-debounce
      :timeout 500
      :action  :dispatch
      :event   [:set-search-debounced search]}]}))

(rf/reg-event-db
 :set-date
 (fn [db [_ date]]
   (assoc db :date date)))

(rf/reg-event-fx
 :search/focus
 (fn [_ _]
   {:dispatch-later [{:ms 10 :dispatch [:set-search-debounce ""]}]
    :focus          "#search"}))

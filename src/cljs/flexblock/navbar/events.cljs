(ns flexblock.navbar.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
 :set-active-page
 (fn [db [_ page]]
   (assoc db :page page)))

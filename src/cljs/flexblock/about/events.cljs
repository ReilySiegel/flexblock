(ns flexblock.about.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
 :about/open?
 (fn [db [_ open?]]
   (assoc db :about/open? open?)))

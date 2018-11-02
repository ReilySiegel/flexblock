(ns flexblock.snackbar.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
 :set-snackbar
 (fn [db [_ message]]
   (assoc db :snackbar message)))

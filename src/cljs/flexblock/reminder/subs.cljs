(ns flexblock.reminder.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :reminder/open
 (fn [db _]
   (:reminder/open db)))

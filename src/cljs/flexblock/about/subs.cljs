(ns flexblock.about.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :about/open?
 (fn [db _]
   (:about/open? db)))

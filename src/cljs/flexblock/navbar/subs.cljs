(ns flexblock.navbar.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :page
 (fn [db _]
   (:page db)))

(rf/reg-sub
 :navbar/options-open
 (fn [db _]
   (:navbar/options-open db)))

(rf/reg-sub
 :navbar/page-zoom
 (fn [db _]
   (:navbar/page-zoom db)))

(ns flexblock.search.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :search
 (fn [db _]
   (:search db)))

(rf/reg-sub
 :search-debounced
 (fn [db _]
   (:search-debounced db)))

(rf/reg-sub
 :date
 (fn [db _]
   (:date db)))

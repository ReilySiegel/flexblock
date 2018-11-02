(ns flexblock.snackbar.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :snackbar
 (fn [db]
   (:snackbar db)))

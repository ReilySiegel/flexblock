(ns flexblock.login.subs
  (:require [re-frame.core :as rf]))


;; The current authorization token.
(rf/reg-sub
 :login/token
 (fn [db _]
   (:login/token db)))


;; The currently logged-in user.
(rf/reg-sub
 :login/user
 (fn [db _]
   (:login/user db)))

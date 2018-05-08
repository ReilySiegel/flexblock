(ns flexblock.events.http
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
 :http/success
 (fn [_ [_ message response]]
   {:notification message}))

(rf/reg-event-fx
 :http/failure
 (fn [_ [_ response]]
   (if-let [message (get-in response [:response :message])]
     {:notification message})))

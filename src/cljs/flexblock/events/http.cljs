(ns flexblock.events.http
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
 :http/success
 (fn [_ [_ message response]]
   {:notification message}))

(rf/reg-event-fx
 :http/failure
 (fn [_ [_ response]]
   (condp = (:status response)
     403 {;; Reload the page after 1000ms.
          :reload 2000
          ;; Show a notification, so the user isn't confused by the reload.
          :notification
          "Client and server are out of sync. This page will be reloaded."}
     {:notification (or (get-in response [:response :message])
                        "Something went wrong.")})))

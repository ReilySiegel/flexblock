(ns flexblock.events.http
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
 :http/success
 (fn [_ [_ message response]]
   {:notification message}))

(rf/reg-event-fx
 :http/failure
 (fn [{:keys [db]} [_ response]]
   (condp = (:status response)
     401 {;; Delete the expired token from the app-db, so that the
          ;; user can reopen the log-in modal if they accidentally
          ;; close it.
          :db           (assoc db :token "")
          ;; Open the login modal.
          :open-modal   "#login-modal"
          ;; Prompt the user to log back in.
          :notification "Your session has expired. Please log in again."}
     403 {;; Reload the page after 2000ms.
          :reload 2000
          ;; Show a notification, so the user isn't confused by the reload.
          :notification
          "Client and server are out of sync. This page will be reloaded."}
     {:notification (or (get-in response [:response :message])
                        "Something went wrong.")})))

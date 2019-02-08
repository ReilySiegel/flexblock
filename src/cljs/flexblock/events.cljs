(ns flexblock.events
  "Global evenets.
  This namespace MUST require all other events namespaces."
  (:require [day8.re-frame.http-fx]
            [clojure.string :as str]
            [flexblock.db :as db]
            [flexblock.login.events]
            [flexblock.navbar.events]
            [flexblock.reminder.events]
            [flexblock.rooms.events]
            [flexblock.search.events]
            [flexblock.snackbar.events]
            [flexblock.users.events]
            [re-frame.core :as rf]
            [re-frame-fx.dispatch]))


(rf/reg-event-db
 :initialize-db
 (fn [db _]
   (merge db
          (db/default-db))))

(rf/reg-event-fx
 :theme/toggle
 (fn [{:keys [db]} _]
   (let [theme (case (:theme db)
                 :dark  :light
                 :light :dark)]
     {:db           (assoc db :theme theme)
      :localstorage {:theme theme}})))

(rf/reg-event-fx
 :http/success
 (fn [_ [_ message response]]
   {:notification message}))



(rf/reg-event-fx
 :http/failure
 (fn [{:keys [db]} [_ response]]
   (condp = (:status response)
     ;; Expired or invalid AUTH token.
     401 {;; Delete the expired token from the app-db, so that the
          ;; user can reopen the log-in modal if they accidentally
          ;; close it.
          :db           (assoc db
                               :token ""
                               :login/open true)
          ;; Prompt the user to log back in.
          :notification "Your session has expired. Please log in again."}
     ;; Expired or invalid CSRF token.
     403 {;; Reload the page after 2000ms.
          :reload 2000
          ;; Show a notification, so the user isn't confused by the reload.
          :notification
          "Client and server are out of sync. This page will be reloaded."}
     ;; Otherwise, show either a message included in the response from
     ;; the server, or a generic error message.
     {:notification (or (get-in response [:response :message])
                        "Something went wrong.")})))

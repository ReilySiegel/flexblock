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
            [flexblock.users.events]
            [re-frame.core :as rf]))


(rf/reg-event-db
 :initialize-db
 (fn [_ _]
   (db/default-db)))


(rf/reg-event-fx
 :set-search
 (fn [{:keys [db]} [_ search]]
   (merge {:db (assoc db :search search)}
          ;; Easter egg referencing RFC-1149.
          (when (= (str/lower-case search) "ipoac")
            {:notification "A pigeon will deliver your packet shortly."})
          ;; Reference CS:GO
          (when (= search "7355608")
            {:notification
             "The bomb has been defused. COUNTER-TERRORISTS win."}))))


(rf/reg-event-db
 :set-date
 (fn [db [_ date]]
   (assoc db :date date)))


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
          :db           (assoc db :token "")
          ;; Open the login modal.
          :open-modal   "#login-modal"
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

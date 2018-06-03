(ns flexblock.notifier.services.email
  (:require
   [mount.core :as mount]
   [flexblock.config :refer [env]]
   [flexblock.notifier.events :as events]
   [postal.core :as postal]))

(mount/defstate email
  :start (:email env))

(defn create [event]
  {:from    (:from email)
   :to      (or (:send-to email)
                (get-in event [:recipient :email]))
   :subject (events/subject event)
   :body    (events/message event)})


(defn send [message]
  (postal/send-message (:smtp email) message))

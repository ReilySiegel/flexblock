(ns flexblock.notifier.services.email
  (:require
   [mount.core :as mount]
   [flexblock.config :refer [env]]
   [flexblock.notifier.services.core :refer [send-notification]]
   [flexblock.notifier.events :as events]
   [postal.core :as postal]))

(mount/defstate email
  :start (get-in env [:notifier :services :email]))

(defn create [event]
  {:from    (:from email)
   :to      (or (:send-to email)
                (get-in event [:recipient :email]))
   :subject (events/subject event)
   :body    (events/message event)})


(defn send [message]
  (postal/send-message (:smtp email) message))

(defmethod send-notification :email [_ event]
  (->> event
       create
       send))

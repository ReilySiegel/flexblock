(ns flexblock.mailer
  (:require
   [mount.core :as mount]
   [flexblock.config :refer [env]]
   [postal.core :as postal]))

(mount/defstate settings
  :start (:email env))

(defn send [message] 
  (postal/send-message (:smtp settings) message))

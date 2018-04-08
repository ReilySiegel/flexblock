(ns flexblock.emailer
  (:require
   [mount.core :as mount]
   [flexblock.config :refer [env]]
   [postal.core :as postal]))

(mount/defstate mailer
  :start (:smtp env))


(defn send [message] 
  (postal/send-message mailer message))

(ns flexblock.notifier.services.core
  (:require [mount.core :as mount]
            [flexblock.config :refer [env]]))

(mount/defstate enabled-services
  "Gets a list of enabled services."
  :start (keys (get-in env [:notifier :services])))

(defmulti send-notification (fn [service event] service))

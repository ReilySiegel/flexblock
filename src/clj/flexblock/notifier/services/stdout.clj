(ns flexblock.notifier.services.stdout
  "A notifier service that logs to STDOUT, used for testing and development."
  (:require [clojure.tools.logging :as log]
            [flexblock.notifier.events :as events]
            [flexblock.notifier.services.core :refer [send-notification]]))

(defmethod send-notification :stdout [_ event]
  (log/info (format  (str "\nNEW NOTIFICATION for %s <%s>:\n"
                          "%s\n\n%s\n\n\n")
                     (get-in event [:recipient :name])
                     (get-in event [:recipient :email])
                     (events/subject event)
                     (events/message event))))

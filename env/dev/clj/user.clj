(ns user
  (:require
   [mount.core :as mount]
   [flexblock.core :refer [start-app]]))

(defn start []
  (mount/start-without #'flexblock.core/repl-server))

(defn stop []
  (mount/stop-except #'flexblock.core/repl-server))

(defn restart []
  (stop)
  (start))

(ns flexblock.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[flexblock started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[flexblock has shut down successfully]=-"))
   :middleware identity})

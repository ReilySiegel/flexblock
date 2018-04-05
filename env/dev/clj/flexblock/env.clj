(ns flexblock.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [flexblock.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[flexblock started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[flexblock has shut down successfully]=-"))
   :middleware wrap-dev})

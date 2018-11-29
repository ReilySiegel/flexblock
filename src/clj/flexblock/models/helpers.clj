(ns flexblock.models.helpers
  (:require [clojure.edn :as edn]
            [toucan.models :as models]))

(def ^:dynamic *master*)

(defn ex-info-assert
  "assert implemented with ExceptionInfo for ease of programmatic use."
  [assertion message]
  (when-not assertion
    (throw (ex-info message {:message message
                             :type    :domain}))))

(defn assert-master
  "Assert that the dynamic variable *master* is set.
  *master* is set to a user responsible for performing some database
  action."
  []
  (ex-info-assert (thread-bound? #'*master*) "*master* is not set."))

(models/add-type! :edn
  :in  pr-str
  :out edn/read-string)

(ns flexblock.interop
  (:require [clojure.string :as str]))

(defn event->value
  "Calls (-> event .-target .-vaule)."
  [event]
  (-> event
      .-target
      .-value))

(defn str->date
  "Converts a date string in ISO8601 format to a js date object."
  [s]
  (->> (str/split s #"-")
       (map #(.parseInt js/window %))
       (apply #(.UTC js/Date %1 %2 %3))
       (js/Date.)))

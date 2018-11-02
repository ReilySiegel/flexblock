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
       ;; Dec month, as JS dates are zero-based.
       (apply #(.UTC js/Date %1 (dec %2) %3))
       (js/Date.)))

(defn date->str [date]
  (-> date
      (.toUTCString)
      ;; Remove time data.
      (str/split #"\d\d:\d\d")
      first
      ;; Remove comma.
      (str/replace #"," "")))

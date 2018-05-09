(ns flexblock.validation
  "Validation and error messages."
  (:require [clojure.spec.alpha :as s]
            [phrase.alpha :as phrase :refer [defphraser]]
            [clojure.string :as str]))

(defn prettify [via & rest]
  (apply str (-> via
                 last
                 name
                 (str/replace #"-" " ")
                 str/capitalize)
         rest))

(defphraser pos-int?
  [_ {:keys [via]}]
  (prettify via " must be a positive integer."))

(defphraser string?
  [_ {:keys [via]}]
  (prettify via " must be a string."))

(defphraser inst?
  [_ {:keys [via]}]
  "A date must be selected.")

(defphraser #(not (str/blank? %))
  [_ {:keys [via]}]
  (prettify via " must not be blank."))

(defphraser #(>= limit (count %))
  [_ {:keys [via]} limit]
  (prettify via " length must be less than " limit "."))

(defphraser #(contains? (hash-set a b c) %)
  [_ {:keys [via]} a b c]
  (prettify via " must be selected."))

(defphraser #(contains? % x)
  [_ {:keys [via]} x]
  "Please fill out all fields.")

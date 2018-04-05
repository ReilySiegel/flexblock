(ns flexblock.validation
  "Validation and error messages."
  (:require [clojure.spec.alpha :as s]))

(s/def :util/<=50 #(<= (count %) 50))
(s/def :util/<=250 #(<= (count %) 250))
(s/def :util/string string?)
(s/def :util/pos pos?)
(s/def :util/integer integer?)
(s/def :util/inst inst?)
(s/def :util/iso-date (partial re-matches
                               #"^\d{4}(/|-|\.)([0]\d|1[0-2])(/|-|\.)([0-2]\d|3[01])$"))

(s/def :room/title (s/and :util/string :util/<=50))
(s/def :room/description (s/and :util/string :util/<=250))
(s/def :room/date (s/and :util/string :util/iso-date))
(s/def :room/number (s/and :util/integer :util/pos))
(s/def :room/max-capacity (s/and :util/integer :util/pos))
(s/def :room/room (s/keys :req-un [:room/title
                                   :room/description
                                   :room/date
                                   :room/number
                                   :room/max-capacity]))
(def errors
  "A mapping of predicates to the errors that should be returned."
  {:util/<=50     "Length must be less than 50."
   :util/<=250    "Length must be less than 250."
   :util/string   "Must be a string."
   :util/pos      "Must be positive." 
   :util/integer  "Must be an integer."
   :util/iso-date "Must be a valid date, formated YYYY/MM/DD"
   :room/room     "All fields are required."})

(defn get-error-message
  [spec val error-map]
  (when-let [problems (::s/problems (s/explain-data spec val))] 
    (->> problems         
         (map (comp #(% error-map "Unknown Error.")
                    last
                    :via))
         first)))


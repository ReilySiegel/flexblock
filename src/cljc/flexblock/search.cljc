(ns flexblock.search
  (:require
   [clojure.string :as str]
   [clj-fuzzy.metrics :as search]))

(defn search-string
  "Fuzzy searches for search in string.
  Splits string into words and computes the distance for each
  word. Does not split search into words."
  [search string]
  (let [search    (str/trim (str/lower-case search))
        string    (str/trim (str/lower-case string))
        inverted? (or (str/starts-with? search "-")
                      (str/starts-with? search "!"))
        search    (if-not inverted? search (subs search 1))
        strings   (str/split string #"\s+")
        num-best  (inc (int (/ (count strings) 5)))
        results   (sort (for [string strings]
                          (search/levenshtein search string)))]
    (when-not (str/blank? search)
      (if inverted?
        (- 10 (/ (reduce + (take num-best results)) num-best))
        (/ (reduce + (take num-best results)) num-best)))))

(ns flexblock.search
  (:require
   [clojure.string :as str]
   [clj-fuzzy.metrics :as search]
   [clj-fuzzy.phonetics :as phonetics]
   [clj-fuzzy.stemmers :as stemmers]))

(def stopwords
  "A set of common words that have no relevance to any particular topic."
  #{"I" "a" "about" "an" "are" "as" "at" "be" "by" "for" "from"  "how" "in" "is"
    "it" "of" "on" "or" "that" "the" "this" "to" "was" "what" "when" "where"
    "who" "will" "with"})

(defn tokenize [s]
  (if-not s
    []
    (remove
     stopwords
     (str/split (str/lower-case s) #"[^a-z]+"))))

(def exact-score
  "The number of points given for an exact match."
  10)
(def metaphone-match-score
  "The number of points given for an exact metapone match"
  7)
(def patial-metaphone-score
  "The number of points given for a partial metaphone match."
  3)

;; Make it possible to use and discard a memoized version of the
;; metaphone function and stem function.
(def ^:dynamic *metaphone-fn* (memoize phonetics/double-metaphone))
(def ^:dynamic *stem-fn* (memoize stemmers/lancaster))


(defn- double-metaphone-score [search search-m word]
  (let [word-m (*metaphone-fn* word)]
    (cond
      ;; Exact match.
      (= word search)
      exact-score

      ;; Exact metaphone match.
      (= word-m search-m)
      metaphone-match-score
      #_#_
      ;; Partial metaphone match.
      (some true?
            (for [word-m-p   word-m
                  search-m-p search-m]
              (= word-m-p search-m-p)))
      patial-metaphone-score

      ;; No match.
      :else 0)))

(defn- score-document
  [tokenized-search tokenized-document]
  (apply + (for [[term term-m] tokenized-search
                 word          tokenized-document]
             (double-metaphone-score term term-m word))))

(defn- make-document-str
  "Returns a string given a string `s` and a weight `n`."
  [weight-map document]
  (str/join " "
            (for [[key weight] weight-map]
              (str/join " " (repeat weight (get document key ""))))))

(defn make-search
  "Takes a `weight-map`, a `search` term, and optionally a `tokenize-fn`.
  The `search` term should be a string.

  Returns a function which takes one `document` as an argument, and
  returns the search score between the `search` term and the
  `document`. The `document` must be a map.

  `tokenize-fn` defaults to `flexblock.search/tokenize`."
  ([weight-map search] (make-search weight-map search tokenize))
  ([weight-map search tokenize-fn]
   (let [tokenized-search (map (fn [search]
                                 [search (*metaphone-fn* search)])
                               (tokenize-fn search))]
     (fn search [document]
       (score-document tokenized-search
                       (tokenize-fn (make-document-str
                                     weight-map
                                     document)))))))

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

(defn tokenize
  ([s] (tokenize s 1 true))
  ([s weight] (tokenize s weight true))
  ([s weight stem?]
   (if-not s
     []
     (let [words (remove stopwords
                         (str/split (str/lower-case s) #"[^a-z]+"))]
       (for [[word count] (frequencies words)]
         (let [stem (stemmers/lancaster word)]
           (merge
            {:weight    (* count weight)
             :exact     word
             :metaphone (phonetics/double-metaphone word)}
            (when stem?
              {:stem           stem
               :stem-metaphone (phonetics/double-metaphone stem)}))))))))

(def exact-score
  "The number of points given for an exact match."
  10)
(def metaphone-match-score
  "The number of points given for an exact metapone match"
  7)
(def patial-metaphone-score
  "The number of points given for a partial metaphone match."
  3)

(defn- score [search-token word-token]
  (let [word-metaphone   (:metaphone word-token)
        search-metaphone (:metaphone search-token)]
    (* (:weight word-token 1)
       (cond
         ;; Exact match.
         (= (:exact word-token)
            (:exact search-token))
         exact-score

         ;; Stem match
         (and (not (nil? (:stem word-token)))
              (= (:stem word-token)
                 (:stem search-token)))
         exact-score

         ;; Exact metaphone match.
         (= word-metaphone
            search-metaphone)
         metaphone-match-score
         ;; Partial metaphone match.
         (not (apply distinct? (concat word-metaphone
                                       search-metaphone)))
         patial-metaphone-score

         ;; No match.
         :else 0))))

(defn score-document
  [tokenized-search tokenized-document]
  (apply + (for [term tokenized-search
                 word tokenized-document]
             (score term word))))

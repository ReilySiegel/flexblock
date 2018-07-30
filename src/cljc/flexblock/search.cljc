(ns flexblock.search
  "The implementation below is *heavily* inspired from this blog post:
  https://thoughts.codegram.com/building-a-content-based-recommender-system-in-clojure/
  Credit for this code, and for the idea of using a TF-IDF system, to Txus."
  (:require
   [clojure.string :as str]
   [clj-fuzzy.metrics :as search]))

(def stopwords
  "A set of common words that have no relevance to any particular topic."
  #{"I" "a" "about" "an" "are" "as" "at" "be" "by" "for" "from"  "how" "in" "is"
    "it" "of" "on" "or" "that" "the" "this" "to" "was" "what" "when" "where"
    "who" "will" "with"})

(defn- tokenize [s]
  (remove
   stopwords
   (str/split (str/lower-case s) #"[^a-zäöüáéíóúãâêîôûàèìòùçñ]+")))

(defn- term-frequencies [tokens]
  (let [freqs      (frequencies tokens)
        term-count (count tokens)]
    (->> freqs
         (map (fn [[term frequency]]
                [term (/ frequency term-count)]))
         (into {}))))

(defn- idf [term corpus]
  (let [documents-matching-term (count (filter #(% term) corpus))]
    (if (pos? documents-matching-term)
      (-> (count corpus)
          (/ documents-matching-term)
          Math/log
          (+ 1))
      1.0)))

(defn- tf-idf [document corpus]
  (->> (term-frequencies document)
       (map (fn [[term freq]]
              [term (* freq (idf term corpus))]))
       (into {})))

(defn- dot-product [document another-document]
  (->> document
       (map (fn [[term tf-idf]]
              (* tf-idf (get another-document term 0.0))))
       (reduce +)))

(defn- magnitude [document]
  (->> document
       (map (fn [[_ tf-idf]]
              (* tf-idf tf-idf)))
       (reduce +)
       Math/sqrt))

(defn- cosine-similarity
  [document another-document]
  (let [dot-p                      (dot-product document another-document)
        document-magnitude         (magnitude document)
        another-document-magnitude (magnitude (select-keys another-document
                                                           (keys document)))
        magnitude-product          (* document-magnitude
                                      another-document-magnitude)]
    (if (zero? magnitude-product)
      0.0
      (/ dot-p magnitude-product))))

(defn search
  "Returns a the cosine-similarity between two strings, given a
  corpus (seq) of strings."
  [search string corpus]
  (let [tokenized-corpus (map (comp set tokenize) corpus)]
    (cosine-similarity (tf-idf (tokenize search) tokenized-corpus)
                       (tf-idf (tokenize string) tokenized-corpus))))

(defn make-search
  "Curried version of `search`, with extra goodies. Takes a seq
  `documents`, and a `search` term. The `search` term should be a
  string. The `documents` should either be a seq of strings, or a
  processing function must be provided to transform the `documents`
  into strings.

  Returns a function which takes one `document` as an argument, and
  returns the cosine similarity between the `search` term and the
  `document`. The `document` must also either be a string, or become a
  string after processing-fn is applied.

  processing-fn defaults to `identity`."
  ([documents search] (make-search documents search identity))
  ([documents search processing-fn]
   (let [corpus        (map (comp set tokenize processing-fn)
                            documents)
         search        (tokenize search)
         search-tf-idf (tf-idf search corpus)]
     (fn [document]
       (cosine-similarity
        search-tf-idf
        (-> document
            processing-fn
            tokenize
            (tf-idf corpus)))))))


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

(ns flexblock.users
  (:require [clojure.string :as str]
            [flexblock.search :as search]))

(defn search [search user] 
  (let [search   (str/trim (str/lower-case search))
        searches (str/split search #"\s+")]
    (reduce + (for [search searches]
                (+ (search/search-string search (:name user))
                   (search/search-string search (:advisor user)))))))

(defn flexblock-on-date?
  "Returns true if user is enrolled in a flexblock on a date."
  [user date]
  (->> user
       :rooms
       (filter #(and
                 (= "flex" (:time %))
                 (= date (:date %))))
       empty?
       not))

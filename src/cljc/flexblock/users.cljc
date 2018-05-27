(ns flexblock.users
  (:require [clojure.string :as str]
            [flexblock.search :as search]
            [clojure.spec.alpha :as s]))

(s/def ::name (s/and string?
                     #(not (str/blank? %))
                     #(>= 50 (count %))))
(s/def ::email (s/and string?
                      #(not (str/blank? %))
                      #(>= 50 (count %))))
(s/def ::class pos-int?)
(s/def ::advisor-id pos-int?)
(s/def ::password string?)

(defmulti user-type #(boolean (or (:teacher %)
                                  (:admin %))))

(defmethod user-type false [_]
  (s/keys :req-un [::name ::email ::class ::advisor-id]
          :opt-un [::password]))

(defmethod user-type true [_]
  (s/keys :req-un [::name ::email]
          :opt-un [::password]))

(s/def ::user (s/multi-spec user-type ::type))

(defn search [search user]
  (let [search   (str/trim (str/lower-case search))
        searches (str/split search #"\s+")]
    (reduce + (for [search searches]
                (+ (search/search-string search (:name user))
                   (search/search-string search (:advisor user "")))))))

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

(defn gen-password [n]
  (apply str
         (take n (repeatedly
                  #(char (+ (rand 26) (rand-nth [97 65])))))))

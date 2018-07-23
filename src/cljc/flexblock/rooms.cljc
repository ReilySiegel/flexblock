(ns flexblock.rooms
  "Contains functions for operating on rooms."
  (:require [flexblock.search :as search]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]))

(s/def ::title (s/and string?
                      #(not (str/blank? %))
                      #(>= 50 (count %))))
(s/def ::description (s/and string?
                            #(not (str/blank? %))
                            #(>= 250 (count %))))
(s/def ::date inst?)
(s/def ::time #(contains? (hash-set "after" "before" "flex") %))
(s/def ::room-number pos-int?)
(s/def ::max-capacity pos-int?)
(s/def ::room (s/keys :req-un [::title
                               ::description
                               ::date
                               ::time
                               ::room-number
                               ::max-capacity]))

(defn get-teacher
  "Given a `room`, returns the teacher."
  [room]
  (->> room
       :users
       (filter :teacher)
       first))

(defn get-students
  "Given a `room`, returns the students in the room."
  [room]
  (->> room
       :users
       (remove :teacher)))

(defn in-room?
  "Given a `room` and a `user-id`, checks if the user is in `room`."
  [room user-id]
  ((->> room
        :users
        (map :id)
        (apply hash-set))
   user-id))

(defn room->str
  "Converts a room map into a string, for searching purposes."
  [room]
  (str/join " " [(:title room)
                 (:description room)
                 (:name (get-teacher room))]))

(defn make-search
  "Returns a function that takes one arg, a room, and returns its search score."
  [rooms search]
  (let [corpus        (map (comp set search/tokenize room->str) rooms)
        search        (search/tokenize search)
        search-tf-idf (search/tf-idf search corpus)]
    (fn [room]
      (search/cosine-similarity
       search-tf-idf
       (-> room
           room->str
           search/tokenize
           (search/tf-idf corpus))))))

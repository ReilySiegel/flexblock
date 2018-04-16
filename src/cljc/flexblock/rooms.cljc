(ns flexblock.rooms
  "Contains functions for operating on rooms."
  (:require [flexblock.search :as search]
            [clojure.string :as str]))

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

(defn search [search room] 
  (let [search   (str/trim (str/lower-case search))
        searches (str/split search #"\s+")]
    (reduce + (for [search searches]
                (+ (search/search-string search (:description room))
                   (search/search-string search (:title room))
                   (search/search-string search (:name (get-teacher room))))))))

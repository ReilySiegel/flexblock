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

(defn time-str [room]
  ;; Make sure time is a keyword.
  (case (keyword (:time room))
    :before "Before School"
    :after  "After School"
    :flex   "FlexBlock"))

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
  "Room-specific version of `flexblock.search/make-search`. Uses
  `room->str` as the processing-fn. See `flexblock.search/make-search`
  for more details."
  [rooms search]
  (search/make-search rooms search room->str))

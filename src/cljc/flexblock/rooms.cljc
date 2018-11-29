
(ns flexblock.rooms
  "Contains functions for operating on rooms."
  (:require [flexblock.search :as search]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [flexblock.primitives :as primitives]))

(def during-schedule
  "Session times that fall within a regular school schedule, excluding
  Flexblock."
  {:a "A Block"
   :b "B Block"
   :c "C Block"
   :d "D Block"
   :e "E Block"
   :f "F Block"
   :g "G Block"
   :h "H Block"})

(def outside-shedule
  "Session times that fall outside a regular school schedule."
  {:before   "Before School"
   :after    "After School"
   :flex     "FlexBlock"
   :advisory "Advisory"})

(def times
  "All times a Session could be scheduled for."
  (merge during-schedule outside-shedule))


(def sorted-times
  "A sorted version of times.
  Note, is stored as a vector of vectors, rather than a map."
  (->> times
       (sort-by second)
       (sort-by #(count (second %)))))

(def attendance->icon
  {1  [:check "Present"]
   0  [:remove "Indeterminate"]
   -1 [:clear "Absent"]
   -2 [:priority_high "Late"]})


(s/def ::id ::primitives/pos-int?)
(s/def ::title (s/and string?
                      #(not (str/blank? %))
                      #(>= 50 (count %))))
(s/def ::description (s/and string?
                            #(not (str/blank? %))
                            #(>= 250 (count %))))
(s/def ::date inst?)
(s/def ::time #(contains? (set (keys times)) (keyword %)))
(s/def ::room-number (s/and string?
                            #(not (str/blank? %))
                            #(>= 25 (count %))))
(s/def ::max-capacity pos-int?)
(s/def ::attendance ::primitives/int?)
(s/def ::users (s/coll-of :flexblock.users/user-single))
(s/def ::room (s/keys :req-un [::title
                               ::description
                               ::date
                               ::time
                               ::room-number
                               ::max-capacity]))

(s/def ::room-hydrated (s/keys :req-un [::title
                                        ::description
                                        ::date
                                        ::time
                                        ::room-number
                                        ::max-capacity
                                        ::users]))

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
  ;; Make sure time is a keyword
  (get times (keyword (:time room)) "Unknown Time"))

(time-str {:time :a})

(defn in-room?
  "Given a `room` and a `user-id`, checks if the user is in `room`."
  [room user-id]
  ((->> room
        :users
        (map :id)
        set)
   user-id))

(defn room-number-str
  "Checks the room-number of a room to see if it is a simple integer,
  and returns an appropriate string."
  [room]
  (let [;; Remove whitespace for comparison.
        room-number (str/replace (:room-number room) #"\s+" "")]
    (if (re-matches #"\d+" room-number)
      (str "Room " (:room-number room))
      (:room-number room))))

(def room-weights
  {:title       2
   :description 1
   :teacher     2})

(defn tokenize [room]
  (apply concat
         (for [[key weight] room-weights]
           (search/tokenize (get room key "") weight))))

(defn make-search
  [search]
  (comp (partial search/score-document (search/tokenize search))
        #(or (:tokens %) (tokenize %))
        ;; Make the teacher's name available to the tokenize function.
        #(assoc % :teacher (:name (get-teacher %)))))


(ns flexblock.rooms
  "Contains functions for operating on rooms."
  (:require [flexblock.search :as search]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]))

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
  ;; Make sure time is a keyword
  (get times (keyword (:time room)) "Unknown Time"))

(time-str {:time :a})

(defn in-room?
  "Given a `room` and a `user-id`, checks if the user is in `room`."
  [room user-id]
  ((->> room
        :users
        (map :id)
        (apply hash-set))
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

(defn make-search
  "Room-specific version of `flexblock.search/make-search`.
  Stems the tokenized words.
  See `flexblock.search/make-search` for more details."
  [search]
  (fn [room]
    ((search/make-search room-weights
                         search
                         (fn [s]
                           (map search/*stem-fn*
                                (search/tokenize s))))
     ;; Add the teacher's name to the room map, so that it can be
     ;; searched.
     (assoc room :teacher (:name (get-teacher room))))))

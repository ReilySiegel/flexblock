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
  (str/join
   (repeatedly n #(char (+ (rand 26) (rand-nth [97 65]))))))

(def roles
  "The roles that users can have in application context.
  This map contains the role keywords mapped to their permission
  level."
  {:student 0
   :teacher 1
   :admin   2})

(defn is-role?
  "Predicate that determines if a user is a role."
  [user role]
  (if-not (= role :student)
    (role user)
    ;; Student role defined as the absense of any other roles.
    (not-any? true? (map (partial is-role? user)
                         (keys (dissoc roles :student))))))

(defn user-roles
  "Returns a vecor of a `user`s roles."
  [user]
  (filter (partial is-role? user) (keys roles)))

(defn highest-role
  "Takes a `user`, and returns their highest-permissioned role ."
  [user]
  (->> user
       user-roles
       (sort-by roles)
       reverse
       first))

(defn can-edit?
  "Returns true if `editor` is allowed to edit `editee`.
  `editor` and `editee` must each have at least one unique field, such
  as an ID or email."
  [editor editee]
  (cond
    ;; A user can always edit themself.
    (= editor editee) true

    ;; Admins can edit other admins.
    (and (= :admin (highest-role editor))
         (= :admin (highest-role editee)))
    true

    ;; Is the highes role of `editor` above the highed role of
    ;; `editee`?
    :else (> (roles (highest-role editor))
             (roles (highest-role editee)))))

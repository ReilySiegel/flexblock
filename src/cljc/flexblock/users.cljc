(ns flexblock.users
  (:require [clojure.string :as str]
            [flexblock.search :as search]
            [flexblock.primitives :as primitives]
            [clojure.spec.alpha :as s]
            [spec-tools.spec :as spec]))

(s/def ::id ::primitives/pos-int?)
(s/def ::name (s/and ::primitives/string?
                     #(not (str/blank? %))
                     #(>= 50 (count %))))
(s/def ::email (s/and ::primitives/string?
                      #(not (str/blank? %))
                      #(>= 50 (count %))))
(s/def ::class ::primitives/pos-int?)
(s/def ::advisor-id (s/or :id ::id
                          :nil ::primitives/nil?))
(s/def ::advisor-name ::name)
(s/def ::password ::primitives/string?)
(s/def ::token ::primitives/string?)
(s/def ::teacher ::primitives/boolean?)
(s/def ::admin ::primitives/boolean?)
(s/def ::rooms (s/coll-of :flexblock.rooms/room))

(defmulti user-type #(boolean (or (:teacher %)
                                  (:admin %))))

(defmethod user-type false [_]
  (s/keys :req-un [::name ::email ::class]
          :opt-un [::password ::advisor-id]))

(defmethod user-type true [_]
  (s/keys :req-un [::name ::email]
          :opt-un [::password]))

(s/def ::user (s/multi-spec user-type ::type))

;; A user not defined as a multi spec. not used for validation.
(s/def ::user-single
  (s/keys :req-un [::name ::email]
          :opt-un [::class ::advisor-id ::teacher ::admin]))

(s/def ::user-hydrated
  (s/keys :req-un [::name ::email ::rooms]
          :opt-un [::class ::advisor-id ::advisor-name ::teacher ::admin]))

(defn flexblock-on-date?
  "Returns true if user is enrolled in a flexblock on a date."
  [user date]
  (->> user
       :rooms
       (filter #(and
                 (=  :flex (:time %))
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
    (or (= editor editee)
        (= (:id editor) (:id editee))) true

    ;; Admins can edit other admins.
    (and (= :admin (highest-role editor))
         (= :admin (highest-role editee)))
    true

    ;; Is the highes role of `editor` above the highed role of
    ;; `editee`?
    :else (> (roles (highest-role editor))
             (roles (highest-role editee)))))

(defn can-delete?
  "Returns true if `deleter` is allowed to edit `deletee`.
  `deletor` and `deletee` must each have at least one unique field, such
  as an ID or email."
  [deleter deletee]
  (and (not= (:id deleter) (:id deletee))
       (can-edit? deleter deletee)))

(def search-weights
  {:name    3
   :email   2
   :advisor 1})

(defn make-search
  [search]
  (search/make-search search-weights search))

(ns flexblock.users-test
  (:require [clojure.test :refer :all]
            [flexblock.users :refer :all :as u]
            [clojure.spec.alpha :as s]))

(deftest user-spec-test
  (let [usera {:id      1
               :name    "User A"
               :email   "user@a.com"
               :teacher true}
        userb {:id         2
               :name       "User V"
               :email      "user@b.com"
               :advisor-id 1
               :class      2020}
        userc {:id    3
               :name  "User C"
               :email "user@c.com"}]
    (testing "User Validation.")
    (is (s/valid? ::u/user usera)
        "Teacher user does not need advisor-id or class.")
    (is (s/valid? ::u/user userb)
        "A student with advisor-id and class is valid.")
    (is (not (s/valid? ::u/user userc))
        "A student must have a class and advisor-id.")))

(deftest search-test
  (let [usera {:name    "A User"
               :advisor "A Advisor"}
        userb {:name    "B User"
               :advisor "B Advisor"}]
    (is (< (search "A" usera) (search "A" userb)))
    (is (> (search "!A" usera) (search "!A" userb))
        "Inverted search.")))

(deftest gen-password-test
  (testing "Gen password."
    (is (string? (gen-password 10))
        "Generates a string.")
    (is (= 10 (count (gen-password 10)))
        "Password is the correct lenght.")))

(deftest can-edit?-test
  (let [admin   {:id 0 :admin true}
        teacher {:id 1 :teacher true}
        student {:id 2}]
    (testing "Admins can edit any user"
      (is (can-edit? admin admin)
          "Admins can edit themself.")
      (is (can-edit? admin (assoc admin :id 9))
          "Admins can edit other admins.")
      (is (can-edit? admin teacher)
          "Admins can edit teachers.")
      (is (can-edit? admin student)
          "Admins can edit students.")
      (is (can-edit? (assoc admin :teacher true) admin)
          ":admin takes precedence over :teacher."))
    (testing "Teachers can edit students."
      (is (can-edit? teacher teacher)
          "Teachers can edit themself.")
      (is (not (can-edit? teacher admin))
          "Teachers cannot edit admins")
      (is (not (can-edit? teacher (assoc teacher :id 9)))
          "Teachers cannot edit other teachers.")
      (is (can-edit? teacher student)
          "Teachers can edit students."))
    (testing "Students can only edit themself."
      (is (can-edit? student student)
          "Students can edit themself.")
      (is (not (can-edit? student (assoc student :id 9)))
          "Students cannot edit other students.")
      (is (not (can-edit? student teacher))
          "Students cannot edit teachers.")
      (is (not (can-edit? student admin))
          "Students cannot edit admins."))))

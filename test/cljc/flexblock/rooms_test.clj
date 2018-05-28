(ns flexblock.rooms-test
  (:require [flexblock.rooms :refer :all :as r]
            [clojure.test :refer :all]
            [clj-time.core :as time]
            [clj-time.coerce :as timec]
            [clojure.spec.alpha :as s]))

(deftest spec-validaiton-test
  (let [room {:title        "Title"
              :description  "A sample desc."
              :date         (timec/to-date
                             (time/now))
              :time         "flex"
              :max-capacity 12
              :room-number  404}]
    (is (s/valid? ::r/room room))))

(deftest get-teacher-test
  (let [teacher {:id 1 :teacher true :name "The Teacher"}
        room    {:users [{:id 0} teacher {:id 2}]}
        room-without-teacher
        {:users [{:id 0}]}]
    (testing "Doesn't explode with nil."
      (is (= nil (get-teacher nil))))
    (testing "Properly identifies the teacher."
      (is (= teacher (get-teacher room))))
    (testing "Doesn't explode if there is no teacher."
      (is (= nil (get-teacher room-without-teacher))))))

(deftest get-students-test
  (let [teacher {:id 1 :teacher true :name "The Teacher"}
        room    {:users [{:id 0} teacher {:id 2}]}
        room-without-students
        {:users [teacher]}]
    (testing "Doesn't explode with nil."
      (is (= [] (get-students nil))))
    (testing "Properly identifies the students."
      (is (= [{:id 0} {:id 2}] (get-students room))))
    (testing "Doesn't explode if there are no students."
      (is (= [] (get-students room-without-students))))))

(deftest in-room?-test
  (let [room {:users [{:id 1} {:id 2}]}]
    (testing "Doesnt explode with nil."
      (is (= nil (in-room? room nil)))
      (is (= nil (in-room? nil 1)))
      (is (= nil (in-room? nil nil))))
    (testing "Correctly identifies if user is in the room."
      (is (in-room? room 1))
      (is (not (in-room? room 3))))))

(deftest search-test
  (let [rooma {:title       "Test"
               :description "A Test Room"
               :users       [{:name "A Teacher" :teacher true}]}
        roomb {:title       "Example"
               :description "An Example Room"
               :users       [{:name "Another Teacher" :teacher true}]}]
    (testing "Correctly searches by title."
      (is (> (search "Example" rooma) (search "Example" roomb)))
      (is (= (search "example" rooma) (search "example" rooma))
          "Search is case insensitive."))
    (testing "Correctly searches by description."
      (is (> (search "Example" rooma) (search "Example" roomb))))
    (testing "Correctly searched by teacher."
      (is (> (search "Another" rooma) (search "Another" roomb))))))

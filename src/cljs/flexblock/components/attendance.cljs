(ns flexblock.components.attendance
  "Render functions related to showing students and taking attendance."
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [flexblock.rooms :as rm]
   [flexblock.components.modal :as modal]))

(defn get-id
  "Given a `room`, returns an ID that will be unique to that `room`."
  [room]
  (str "attendance" (:id room)))

(defn- student
  "One student in the attendance list."
  [user]
  [:li.collection-item
   {:key (:id user)}
   [:div (:name user)]])

(defn modal
  "The bottom sheet modal that shows a list of students."
  [room]
  (let [students (rm/get-students room)]
    ^{:key (:id room)}
    [modal/bottom-sheet (get-id room)
     [:div.modal-content
      [:h4.purple-text.text-lighten-3 "Students"]
      [:div.row
       [:div.col.l8.offset-l2.s12
        (if (seq students)
          [:ul.collection
           (map student students)]
          [:h6.amber-text.center "No students have joined yet."])]]]]))

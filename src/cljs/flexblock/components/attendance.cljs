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

(defn- modal
  "The bottom sheet modal that shows a list of students."
  [room]
  (let [students (rm/get-students room)]
    [modal/bottom-sheet (get-id room)
     [:div.modal-content
      [:h4.purple-text.text-lighten-3 "Students"]
      [:div.row
       [:div.col.l8.offset-l2.s12
        (if (seq students)
          [:ul.collection
           (map student students)]
          [:h6.amber-text.center "No students have joined yet."])]]]]))

(defn attendance
  "A simple wrapper for `flexblock.components.attendance/modal`.
  Because `flexblock.components.attendance/modal` uses
  `react.core/create-class`, it cannot be used as a function in
  map. This wrapper function allows for use in map by returning simple
  hiccup."
  [room]
  [modal (assoc room :key (:id room))])

(ns flexblock.pages.students
  (:require [flexblock.components.students :as students]
            [flexblock.components.emailer :as emailer]))

(defn page
  []
  [:div
   [emailer/fab]
   [emailer/modal]
   [students/fab]
   [students/add]
   [students/grid]])

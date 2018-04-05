(ns flexblock.pages.students
  (:require [flexblock.components.students :as students]))

(defn page
  []
  [students/grid])

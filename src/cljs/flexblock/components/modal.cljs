(ns flexblock.components.modal
  (:require [reagent.core :as r]))

(defn- base
  "A generic modal.
  Can be built on by adding css `class`es. `children` chold be an array
  of child elements."
  [id class children]
  (r/create-class
   {:component-did-mount #(let [e (.getElementById js/document id)]
                            (.init js/M.Modal e)) 
    :reagent-render
    (fn [id class children]
      (into [:div.modal {:id         id
                         :class-name class}]
            children))}))

(defn standard
  "A modal with a fixed footer."
  [id & children]
  [base id "" children])

(defn fixed-footer
  "A modal with a fixed footer."
  [id & children]
  [base id "modal-fixed-footer" children])

(defn bottom-sheet
  "A modal with a fixed footer."
  [id & children]
  [base id "bottom-sheet" children])

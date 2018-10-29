(ns flexblock.components.modal
  (:require [reagent.core :as r]))

(defn- base
  "A generic modal.
  Can be built on by adding css `class`es. `children` is an array of
  child elements."
  [opts class children]
  (let [{:keys [id on-close on-open]
         :or   {id       (name (gensym "modal"))
                on-close (fn [])
                on-open  (fn [])}} opts]
    (r/create-class
     {:component-did-mount #(let [e (.getElementById js/document id)]
                              (.init js/M.Modal e
                                     (clj->js
                                      {:onCloseEnd on-close
                                       :onOpenEnd  on-open})))
      :reagent-render
      (fn [opts class children]
        (into [:div.modal {:id    id
                           :class class}]
              children))})))

(defn standard
  "A modal with a fixed footer."
  [opts & children]
  [base opts "" children])

(defn fixed-footer
  "A modal with a fixed footer."
  [opts & children]
  [base opts "modal-fixed-footer" children])

(defn bottom-sheet
  "A modal with a fixed footer."
  [opts & children]
  [base opts "bottom-sheet" children])

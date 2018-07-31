(ns flexblock.components.konami
  "Konami Code Easter Egg."
  (:require [reagent.core :as r]))

(def konami-code
  "A sequence of keycodes that defines the Konami Code"
  ["ArrowUp" "ArrowUp" "ArrowDown" "ArrowDown" "ArrowLeft"
   "ArrowRight" "ArrowLeft" "ArrowRight" "KeyB" "KeyA"])

(defn cycle-keys [keys new-key]
  (if (> 10  (count keys))
    (conj keys new-key)
    (conj (vec (rest keys)) new-key)))

(defn egg []
  (let [keys     (r/atom [])
        listener (fn [e] (swap! keys cycle-keys (.-code e)))]
    (r/create-class
     {:component-did-mount
      (fn []
        (.addEventListener js/document
                           "keypress"
                           listener))
      :component-will-unmount
      #(.removeEventListener js/document "keypress" listener)
      :reagent-render
      (fn []
        (when (= @keys konami-code)
          (.toast js/M (clj->js
                        {:html "<span class=\"green-text\">♥</span>"}))
          nil))})))

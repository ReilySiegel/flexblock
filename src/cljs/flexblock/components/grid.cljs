(ns flexblock.components.grid
  (:require [reagent.core :as r]))

(defn grid
  "Returns a masonry grid `children` as items.
  Children should be a sequence of other components. These children
  should have the class grid-item."
  [children]
  (let [id     (name (gensym "grid-"))
        layout (fn layout [& args]
                 (new js/Masonry
                      (.getElementById js/document id)
                      (clj->js {:itemSelector    ".grid-item"
                                :horizontalOrder true})))]
    (r/create-class
     {:component-did-mount  layout
      :component-did-update layout
      :reagent-render
      (fn [children]
        (if (seq children)
          [:div.row
           {:id id}
           children]
          [:div {:id id}]))})))

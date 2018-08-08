(ns flexblock.components.beta
  "This namespace contains a disclaimer that appears on page load.
  It takes this disclaimer from the JS variable betaDisclaimer, which
  should be set by the server."
  (:require
   [reagent.core :as r]))

(def disclaimer-text
  "The text of the disclaimer."
  (.-betaDisclaimer js/window))

(defn disclaimer []
  (r/create-class
   {:component-did-mount
    (fn []
      (let [e (.getElementById js/document "beta-disclaimer")]
        (when e
          (.open (.init js/M.Modal e)))))
    :reagent-render
    (fn []
      (when-not (empty? disclaimer-text)
        [:div.modal.modal-fixed-footer
         {:id :beta-disclaimer}
         [:div.modal-content
          [:p.flow-text disclaimer-text]]
         [:div.modal-footer
          [:a.btn-flat.amber-text.waves-effect.waves-red.modal-close
           {:on-click #(-> js/window
                           .-location
                           .-href
                           ;; If they don't want to use Flexblock, they
                           ;; deserve to be rickrolled.
                           (set! "https://youtu.be/dQw4w9WgXcQ?t=43s"))}
           "No Thanks"]
          [:a.btn-flat.amber-text.waves-effect.waves-purple.modal-close
           "Let's Go!"]]]))}))

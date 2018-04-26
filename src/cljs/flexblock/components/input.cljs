(ns flexblock.components.input
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [flexblock.validation :as v]
            [clojure.string :as str]))

(defn input-rf-dispatch
  ([opts label dispatch-key subscribe-key]
   (input-rf-dispatch opts label dispatch-key subscribe-key false false))
  ([opts label dispatch-key subscribe-key validate]
   (input-rf-dispatch opts label dispatch-key subscribe-key validate false))
  ([opts label dispatch-key subscribe-key validate on-change]
   (r/create-class
    {:component-did-mount #(.updateTextFields js/M)
     :reagent-render
     (fn
       render
       ([opts label dispatch-key subscribe-key]
        (render opts label dispatch-key subscribe-key false false)) 
       ([opts label dispatch-key subscribe-key validate]
        (render opts label dispatch-key subscribe-key validate false))
       ([opts label dispatch-key subscribe-key validate on-change] 
        (let [id          (str (gensym "formid"))
              val         @(rf/subscribe [subscribe-key])
              error       (when validate
                            (v/get-error-message subscribe-key val v/errors))
              show-error? (not (or (str/blank? val)
                                   (not validate)
                                   (nil? val)
                                   (zero? val)))]
          [:div.input-field
           [(if (:textarea opts) :textarea :input)
            (-> opts
                (dissoc :textarea)
                (assoc :id id)
                (assoc :default-value (str val))
                (update :class-name str (if show-error?
                                          " validate"))
                (update :class-name str (if show-error?
                                          (if error
                                            " invalid")) )
                (assoc (if on-change :on-change :on-blur)
                       #(rf/dispatch
                         [dispatch-key
                          (-> % .-target .-value)])))]
           [:span.helper-text {:data-error (str error)}]
           #_[:label {:for id} label]])))})))



(defn datepicker
  "Returns a datepicker object.
  Can take an optional argument `id`. `id` will be constructed from a
  gensym if excluded (a safe default). If you need to refer to the
  datepicker by #id, you must provide a UNIQUE identifier."
  [opts]
  (let [{:keys [id dispatch-key]
         :or   {id (str (gensym "datepicker"))}}
        opts
        datepicker-el (atom nil)]
    (r/create-class
     {:display-name "datepicker"
      :component-did-mount
      (fn []
        (reset! datepicker-el
                (.init js/M.Datepicker
                       (.getElementById js/document id)
                       (-> {:container       "#app"
                            :disableWeekends true
                            :yearRange       1}
                           (merge (when dispatch-key
                                    {:onClose
                                     #(rf/dispatch
                                       [dispatch-key
                                        (.-date @datepicker-el)])}))
                           clj->js))))
      :reagent-render
      (fn [opts]
        [:input.datepicker
         {:id id}])})))

(defn clear-selector [selector]
  (when-let [e (array-seq (.querySelectorAll js/document selector))] 
    (doall (map #(set! (.-value %) "") e))))

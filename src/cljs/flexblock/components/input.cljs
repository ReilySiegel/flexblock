(ns flexblock.components.input
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [flexblock.validation :as v]
            [clojure.string :as str]
            [goog.string :as gstring]
            [goog.string.format]))

(defn text
  [{:keys [id placeholder class-name type
           dispatch-key subscribe-key
           on-change? validate? textarea?]
    :or   {id (str (gensym "formid"))}}]
  (let [val         (when subscribe-key (rf/subscribe [subscribe-key]))
        val-empty?  (or (str/blank? @val) 
                        (nil? @val)
                        (zero? @val))
        error       (when validate? (v/get-error-message subscribe-key @val v/errors))
        show-error? (not (or validate?
                             val-empty?))]
    [:div.input-field
     [(if textarea? :textarea :input)
      {:id             id
       (if type :type) type
       :default-value  (str @val)
       :placeholder    placeholder

       :class-name (str class-name 
                        (if show-error?
                          (if error
                            " validate invalid"
                            " validate")))
       (when dispatch-key
         (if on-change? :on-change :on-blur))
       #(rf/dispatch
         [dispatch-key (-> % .-target .-value)])}]
     [:span.helper-text {:data-error (str error)}]]))

(defn datepicker
  "Returns a datepicker object.
  Can take an optional argument `id`. `id` will be constructed from a
  gensym if excluded (a safe default). If you need to refer to the
  datepicker by #id, you must provide a UNIQUE identifier."
  [opts]
  (let [{:keys [id dispatch-key subscribe-key placeholder]
         :or   {id          (str (gensym "datepicker"))
                placeholder "Date"}}
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
         (-> {:id          id
              :placeholder placeholder} 
             (merge
              (when subscribe-key
                (when-let [date @(rf/subscribe [subscribe-key])]
                  (when-not (nil? date)
                    (println date)
                    {:default-value (->> (str/split (.toDateString date) #" ")
                                         rest 
                                         (apply gstring/format
                                                "%s %s, %s"))})))))])})))

(defn clear-selector [selector]
  (when-let [e (array-seq (.querySelectorAll js/document selector))] 
    (doall (map #(set! (.-value %) "") e))))

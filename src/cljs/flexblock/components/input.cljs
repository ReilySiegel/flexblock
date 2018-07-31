(ns flexblock.components.input
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [flexblock.validation :as v]
            [clojure.string :as str]
            [goog.string :as gstring]
            [goog.string.format]
            [phrase.alpha :as phrase]
            [cljs.reader :as reader]))

(defn focus
  "Sets focus to the input with the provided id."
  [id]
  (.focus (.getElementById js/document id)))

(defn text
  [{:keys [id placeholder class-name type atom
           dispatch-key subscribe-key textarea?]
    :or   {id   (str (gensym "formid"))
           atom (r/atom "")}}]
  (fn [_]
    [:div.input-field
     [(if textarea? :textarea :input)
      {:id             id
       (if type :type) type
       :value          (if subscribe-key
                         (str @(rf/subscribe [subscribe-key]))
                         (str @atom))
       :placeholder    placeholder
       :on-change      (if dispatch-key
                         #(rf/dispatch [dispatch-key (-> % .-target .-value)])
                         #(reset! atom (-> % .-target .-value)))}]]))

(defn checkbox
  [{:keys [atom label]
    :or   {atom (r/atom false)}}]
  (fn
    [_]
    [:label
     [:input {:type      :checkbox
              :checked   @atom
              :on-change #(reset! atom (-> %
                                           .-target
                                           .-checked))}]
     (when label [:span label])]))

(defn select
  [{:keys [id atom placeholder options]
    :or   {atom (r/atom nil)
           id   (name (gensym "select"))}}]
  (r/create-class
   {:component-did-mount
    (fn []
      (.init js/M.Select
             (.getElementById js/document id)))
    :reagent-render
    (fn [_]
      (into [:select
             {:id        id
              :on-change #(reset! atom
                                  (-> %
                                      .-target
                                      .-value
                                      reader/read-string))
              :value     (or @atom "")}]
            (conj (for [option options]
                    ^{:key option}

                    (if (map? option)
                      ;; If the option is a map, use :value and :label vals.
                      [:option {:value (prn-str (:value option))}
                       (str (:label option))]
                      ;; If not, use the option itself as val and label.
                      [:option {:value (prn-str option)} (str option)]))
                  (when placeholder
                    [:option {:value    ""
                              :disabled true} placeholder]))))}))

(defn datepicker
  "Returns a datepicker object.
  Can take an optional argument `id`. `id` will be constructed from a
  gensym if excluded (a safe default). If you need to refer to the
  datepicker by #id, you must provide a UNIQUE identifier."
  [opts]
  (let [{:keys [id atom placeholder subscribe-key dispatch-key]
         :or   {id          (str (gensym "datepicker"))
                placeholder "Date"
                atom        (r/atom nil)}}
        opts
        datepicker-el (clojure.core/atom nil)]
    (r/create-class
     {:display-name "datepicker"
      :component-did-mount
      (fn []
        (reset! datepicker-el
                (.init js/M.Datepicker
                       (.getElementById js/document id)
                       (clj->js
                        {:container       "#app"
                         :disableWeekends true
                         :yearRange       1
                         :onClose
                         #(let [unsafe-date
                                (.-date @datepicker-el)
                                ;; Remove any time data that slips
                                ;; though.  This can be caused by
                                ;; clicking the 'today' button on the
                                ;; datepicker.
                                date (when unsafe-date
                                       (js/Date.
                                        (.setHours unsafe-date 0 0 0 0)))]
                            (if dispatch-key
                              (rf/dispatch [dispatch-key date])
                              (reset! atom date)))}))))
      :reagent-render
      (fn [opts]
        [:input.datepicker
         {:id          id
          :placeholder placeholder
          :value       (if-not (inst? @atom)
                         ""
                         (->> (str/split (.toDateString
                                          (if subscribe-key
                                            @(rf/subscribe [subscribe-key])
                                            @atom))
                                         #" ")
                              rest
                              (apply gstring/format
                                     "%s %s, %s")))}])})))

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
  "A text input. Tales a map of options. All options are optional.

  Options:

  :id            Sets the id of the <input> element.
  :placeholder   Sets a placeholder for the <input>.
  :type          Sets the type of the <input>.
  :atom          Must be a reagent atom. All changes to the atom are applied to
                 the <input>, and all changes to the <input> are applied to the
                 atom.
  :subscribe-key A re-frame subscription key, to which the value of the <input>
                 will be set on render. Takes precedent over :atom, as only one
                 can be used.
  :dispatch-key  A re-frame event key, to which the value of the <input> will be
                 sent on-change. Takes precedent over :atom, as only one can
                 be used.
  :textarea?     If true, uses a <textarea> instead of an <input>."
  [{:keys [id placeholder type atom dispatch-key subscribe-key textarea?]
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
  "A checkbox input. Tales a map of options. All options are optional.

  Options:

  :label Sets a label for the checkbox.
  :atom  Must be a reagent atom. All changes to the atom are applied to the
         checkbox, and all changes to the checkbox are applied to the atom."
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
  "A select input. Takes a map of options. All options are technically
  optional, but you probably want to include :options.

  Options:

  :id          The id for the <select> element.
  :atom        Must be an reagent atom. All changes to the value of <select>
               are reflected in the atom, and all external changes to the atom
               are reflected in the value of the <select> element.
  :placeholder A disabled option that is shown by default.
  :options     A sequence (list vector seq...) of options to display in the
               <select> element. An option may either be a value x, in which
               case the value of the <option> will be x, and the string
               displayed will be (str x), or a map of the form
               {:value x, :label y},  where the value of the <option> will be
               x, and the string displayed will be (str y)."
  [{:keys [id atom placeholder options]
    :or   {atom (r/atom nil)
           id   (name (gensym "select"))}}]
  (let [select-el (cljs.core/atom nil)]
    (r/create-class
     {:component-did-mount
      (fn []
        (reset! select-el (.init js/M.Select
                                 (.getElementById js/document id))))
      :component-did-update
      (fn []
        (.destroy (deref select-el))
        (reset! select-el
                (.init js/M.Select
                       (.getElementById js/document id))))
      :reagent-render
      (fn [_]
        (into [:select
               {:id        id
                :on-change #(reset! atom
                                    (-> %
                                        .-target
                                        .-value
                                        reader/read-string))
                :value     (if @atom
                             (prn-str @atom)
                             "")}]
              (conj (for [option options]
                      ^{:key option}

                      (if (map? option)
                        ;; If the option is a map, use :value and :label
                        ;; vals. prn-str is used to retain the exact
                        ;; Clojure value, even though it would otherwise
                        ;; be converted to a string.
                        [:option {:value (prn-str (:value option))}
                         (str (:label option))]
                        ;; If not, use the option itself as val and label.
                        [:option {:value (prn-str option)} (str option)]))
                    (when placeholder
                      [:option {:value    ""
                                :disabled true} placeholder]))))})))

(defn datepicker
  "A datepicker input. Tales a map of options. All options are optional.

  Options:

  :id            Sets the id of the <input> element.
  :placeholder   Sets a placeholder for the <input>.
  :atom          Must be a reagent atom. All changes to the atom are applied to
                 the <input>, and all changes to the <input> are applied to the
                 atom.
  :subscribe-key A re-frame subscription key, to which the value of the <input>
                 will be set on render. Takes precedent over :atom, as only one
                 can be used.
  :dispatch-key  A re-frame event key, to which the value of the <input> will be
                 sent on-change. Takes precedent over :atom, as only one can
                 be used."
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
                                ;; though. This can be caused by
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

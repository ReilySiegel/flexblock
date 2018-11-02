(ns flexblock.components.material
  (:require [camel-snake-kebab.core :as case]
            [clojure.set :as set]
            [material-ui]
            [reagent.core :as r]
            [reagent.impl.template :as rtpl])
  (:require-macros
   [flexblock.components.macros
    :refer [export-material-ui-react-classes]]))

(defn- update-keys [m f]
  (set/rename-keys m
                   (apply merge (map (fn [key]
                                       {key (f key)})
                                     (keys m)))))

(defn- update-props [props]
  (-> props
      (update-keys case/->camelCaseKeyword)))

(defn color [color]
  (aget js/MaterialUIColors (name color)))

(defn createMuiTheme [theme]
  ((aget js/MaterialUIStyles "createMuiTheme") (clj->js theme)))

(def MuiThemeProvider (-> js/MaterialUIStyles
                          (aget "MuiThemeProvider")
                          (reagent.core/adapt-react-class)))

(def ^:private input-component
  (r/reactify-component
   (fn [props]
     [:input (-> props
                 (assoc :ref (:inputRef props))
                 (dissoc :inputRef))])))

(def ^:private textarea-component
  (r/reactify-component
   (fn [props]
     [:textarea (-> props
                    (assoc :ref (:inputRef props))
                    (dissoc :inputRef))])))

(defn TextField [props & children]
  (let [props
        (-> props
            (assoc-in [:InputProps :inputComponent]
                      (cond
                        (and (:multiline props)
                             (:rows props)
                             (not (:maxRows props)))
                        textarea-component

                        ;; FIXME: Autosize multiline field is broken.
                        (:multiline props)
                        nil

                        ;; Select doesn't require cursor fix so
                        ;; default can be used.
                        (:select props)
                        nil

                        :else
                        input-component))
            rtpl/convert-prop-value)]
    (apply r/create-element
           (aget js/MaterialUI "TextField")
           props
           (map r/as-element children))))


(export-material-ui-react-classes)

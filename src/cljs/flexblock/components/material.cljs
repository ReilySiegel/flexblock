(ns flexblock.components.material
  (:require [camel-snake-kebab.core :as case]
            [clojure.set :as set]
            [material-ui]
            [reagent.core])
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

(export-material-ui-react-classes)

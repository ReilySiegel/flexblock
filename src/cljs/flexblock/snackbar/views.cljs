(ns flexblock.snackbar.views
  (:require [flexblock.components.material :as material]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [clojure.string :as str]))

(defn snackbar []
  (let [message @(rf/subscribe [:snackbar])]
    [material/Snackbar
     {:open             (not (nil? message))
      :autoHideDuration 2000
      :onClose          (fn []
                          (rf/dispatch-sync [:set-snackbar nil]))
      :message          (str message)}]))

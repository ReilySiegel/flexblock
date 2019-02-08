(ns flexblock.reminder.views
  (:require [clojure.string :as str]
            [flexblock.components.material :as material]
            [re-frame.core :as rf]
            [goog.string :as gstring]
            [goog.string.format]))

(defn button
  "Button to open reminder modal."
  []
  (when (and
         (:admin @(rf/subscribe [:login/user]))
         (not (str/blank? @(rf/subscribe [:login/token]))))
    [material/Button
     {:fullWidth true
      :onClick   #(rf/dispatch [:reminder/set-open true])}
     "Reminder"]))

(defn modal
  "The modal to display the emailer."
  []
  (let [users (rf/subscribe [:users/filtered])
        date  (rf/subscribe [:date])]
    [material/Dialog
     {:fullWidth true
      :scroll    :paper
      :open      @(rf/subscribe [:reminder/open])
      :onClose   #(rf/dispatch [:reminder/set-open false])}
     [material/DialogTitle "Reminder"]
     [material/DialogContent
      (if (str/blank? @date)
        [material/Typography
         {:variant :subtitle1}
         "No Date Selected"]
        (if (zero? (count @users))
          [material/Typography
           {:variant :subtitle1}
           "All Students are enrolled."]
          [:div
           [material/Typography
            {:variant :subtitle1}
            (gstring/format
             "Are you sure you want to send a reminder to the following %s users?"
             (count @users))]
           [material/Typography
            (apply str (->> @users
                            (map :name)
                            (interpose ", ")))]]))]
     [material/DialogActions
      [material/Button
       {:color    :secondary
        :disabled (or (empty? @date)
                      (zero? (count @users)))
        :onClick  #(rf/dispatch [:reminder/post-date])}
       "Send Reminder"]]]))

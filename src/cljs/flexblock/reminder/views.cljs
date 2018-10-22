(ns flexblock.reminder.views
  (:require [clojure.string :as str]
            [flexblock.components.material :as material]
            [flexblock.components.modal :as modal]
            [re-frame.core :as rf]))

(defn button
  "Button to open reminder modal."
  []
  (when (and
         (:admin @(rf/subscribe [:login/user]))
         (not (str/blank? @(rf/subscribe [:login/token]))))
    [material/Button
     {:color     :secondary
      :fullWidth true
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
            "Are you sure you want to send a reminder to the following users?"]
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

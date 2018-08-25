(ns flexblock.reminder.views
  (:require [clojure.string :as str]
            [flexblock.components.modal :as modal]
            [re-frame.core :as rf]))

(defn button
  "Button to open reminder modal."
  []
  (when (and
         (:admin @(rf/subscribe [:login/user]))
         (not (str/blank? @(rf/subscribe [:login/token]))))
    [:a.btn-flat.amber-text.modal-trigger
     {:href "#reminder-modal"}
     "Reminder"]))

(defn modal
  "The modal to display the emailer."
  []
  (let [users (rf/subscribe [:users/filtered])
        date  (rf/subscribe [:date])]
    [modal/fixed-footer {:id "reminder-modal"}
     [:div.modal-content
      [:h4.center.purple-text.text-lighten-3 "Reminder"]
      (if (str/blank? @date)
        [:h6.amber-text
         "No Date Selected"]
        (if (zero? (count @users))
          [:h6.amber-text
           "All Students are enrolled."]
          [:div
           [:h6.amber-text
            "Are you sure you want to send a reminder to the following stundents?"]
           [:div
            [:p (apply str (->> @users
                                (map :name)
                                (interpose ", ")))]]]))]
     [:div.modal-footer
      [:a.btn-flat.amber-text.waves-effect.waves-purple
       {:disabled (zero? (count @users))
        :on-click #(rf/dispatch [:mailer/post-date])}
       "Send Reminder"]]]))

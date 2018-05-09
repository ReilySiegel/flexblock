(ns flexblock.components.emailer
  (:require
   [clojure.string :as str]
   [flexblock.components.modal :as modal]
   [flexblock.users :as users]
   [re-frame.core :as rf]))

(defn fab
  "FAB to open emailer modal."
  [] 
  (when (and
         (:admin @(rf/subscribe [:user]))
         (not (str/blank? @(rf/subscribe [:token])))) 
    [:a.btn-flat.amber-text.modal-trigger
     {:href "#emailermodal"}
     "Reminder"]))

(defn modal
  "The modal to display the emailer."
  [] 
  (let [users    (rf/subscribe [:users])
        date     (rf/subscribe [:date])
        students (->> @users
                      (remove :teacher)
                      (remove #(users/flexblock-on-date? % @date))
                      (sort-by :name))] 
    [modal/fixed-footer "emailermodal"
     [:div.modal-content
      [:h4.center.purple-text.text-lighten-3 "Emailer"] 
      (if (str/blank? @date)
        [:h6.amber-text
         "No Date Selected"]
        (if (zero? (count students))
          [:h6.amber-text
           "All Students are enrolled."]
          [:div
           [:h6.amber-text
            "Are you sure you want to send mail to the following stundents?"]
           [:div
            [:p (apply str (->> students
                                (map :name)
                                (interpose ", ")))]]]))]
     [:div.modal-footer
      [:a.btn-flat.amber-text.waves-effect.waves-purple
       {:disabled (zero? (count students))
        :on-click #(rf/dispatch [:mailer/post-date])}
       "Send Mail"]]]))

(ns flexblock.components.emailer
  (:require
   [clojure.string :as str]
   [flexblock.components.modal :as modal]
   [flexblock.rooms :as rooms]
   [re-frame.core :as rf]))

(defn fab
  "FAB to open emailer modal."
  [] 
  (when (and
         (:teacher @(rf/subscribe [:user]))
         (not (str/blank? @(rf/subscribe [:token]))))
    [:div {:style {:position :fixed
                   :right    24
                   :bottom   24}}
     [:a.btn-floating.btn-large.amber.hoverable.modal-trigger
      {:href "#emailermodal"}
      [:i.large.material-icons "email"]]]))

(defn modal
  "The modal to display the emailer."
  [] 
  (let [users    (rf/subscribe [:users])
        students (->> @users
                      (remove :teacher)
                      (map #(update % :rooms rooms/get-flexblock-on-date
                                    @(rf/subscribe [:date])))
                      (filter #(empty? (:rooms %)))
                      (sort-by :name))] 
    [modal/fixed-footer "emailermodal"
     [:div.modal-content
      [:h4.center.purple-text.text-lighten-3 "Emailer"] 
      (if (str/blank? @(rf/subscribe [:date]))
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
       {:disabled (zero? (count students))}
       "Send Mail"]]]))

(ns flexblock.components.students
  (:require [reagent.core :as r]
            [re-frame.core :as rf] 
            [flexblock.rooms :as rooms]
            [flexblock.users :as user]
            [flexblock.utils :as u]
            [flexblock.components.search :as search]
            [flexblock.components.modal :as modal]
            [flexblock.components.input :as input]))

(defn- buttons
  "Returns the appropriate actions that a user can take on a `room`."
  [user]
  (let [{:keys [id name rooms]}
        user]
    [:div.card-action 
     [:a.btn-flat.amber-text.waves-effect.waves-purple.modal-trigger
      {:href (str "#sessionmodal" (:id user))} "Sessions"]
     [:a.btn-flat.amber-text.waves-effect.waves-purple.modal-trigger
      {:href (str "#passwordmodal" (:id user))} "Reset Password"]]))

(defn- session
  "One session in :rooms list."
  [room] 
  [:li.collection-item
   {:key (:id room)}
   [:div (str (:title room) "  - "
              (:name (rooms/get-teacher room)) " - "
              ((keyword (:time room)) {:before "Before School"
                                       :after  "After School"
                                       :flex   "FlexBlock"} "") " "
              (:date room))]])

(defn password-modal
  "Shows a modal that allows a teacher to change the password of `user`."
  [user]
  ^{:key (:id user)}
  [modal/standard (str "passwordmodal" (:id user))
   [:div.modal-content
    [:h4.center.purple-text.text-lighten-3
     (str "Reset Password for " (:name user))]
    [:div.row [:div.col.l6.offset-l3.m12
               [input/input-rf-dispatch
                {:type        :password
                 :placeholder "New Password"}
                "Password"
                :set-reset-password
                :reset-password]]]]
   [:div.modal-footer
    [:a.btn-flat.amber-text.waves-effect.waves-purple
     {:on-click #(u/set-password (:id user))}
     "Reset Password"]]])

(defn modal
  "The bottom sheet modal that shows a list of students."
  [user]
  (let [sessions (:rooms user)]
    ^{:key (:id user)}
    [modal/bottom-sheet (str "sessionmodal" (:id user)) 
     [:div.modal-content
      [:h4.purple-text.text-lighten-3 "Sessions"]
      [:div.row
       [:div.col.l8.offset-l2.s12
        (if (seq sessions)
          [:ul.collection
           (map session (->> sessions
                             (sort-by :date)
                             reverse))]
          [:h6.amber-text.center
           "This Student is not enrolled in any Sessions."])]]]]))

(defn card
  "Creates a card with information about a `room`."
  [user]
  (when-let [{:keys [id name rooms]} user] 
    [:div.col.s12.m6.l4.grid-item
     {:key id} 
     [:div.card.hoverable
      [:div.card-content
       [:span.card-title.truncate name]
       [:p (:advisor user)]]
      [:div.divider] 
      [buttons user]]]))


(defn grid
  "Returns a grid of students."
  []
  (let [users (rf/subscribe [:users])]
    (if (seq @users)
      (let [date     @(rf/subscribe [:date])
            students (->> @users
                          (remove :teacher)
                          (sort-by :name) 
                          (sort-by #(user/search @(rf/subscribe [:search]) %)))]
        [:div.container
         [search/search-bar]
         [search/date-bar]
         [:div.row.grid-user
          (doall
           (if (empty? date)
             (map card students)
             (map card (->> students
                            (map #(update % :rooms rooms/get-flexblock-on-date date))
                            (filter #(empty? (:rooms %)))))))
          (doall (map modal students))
          (doall (map password-modal students))]])
      [:div.grid-user])))

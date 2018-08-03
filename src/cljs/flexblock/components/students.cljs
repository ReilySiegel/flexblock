(ns flexblock.components.students
  (:require
   [clojure.string :as str]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [flexblock.rooms :as rooms]
   [flexblock.users :as user]
   [flexblock.components.emailer :as emailer]
   [flexblock.components.grid :as grid]
   [flexblock.components.search :as search]
   [flexblock.components.modal :as modal]
   [flexblock.components.input :as input]
   [flexblock.components.password :as password]))

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
              (.toDateString (:date room)) " - "
              "Room " (:room-number room))]])

(defn modal
  "The bottom sheet modal that shows a list of students."
  [user]
  (let [sessions (:rooms user)]
    ^{:key (:id user)}
    [modal/bottom-sheet {:id (str "sessionmodal" (:id user))}
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

(defn get-years [date]
  (let [year  (.getFullYear date)
        month (.getMonth date)]
    (if (>= month 7)
      (range (inc year) (+ 5 year))
      (range year (+ 4 year)))))

(defn add
  "The form for creating a new user."
  []
  (let [name     (r/atom "")
        email    (r/atom "")
        teacher? (r/atom false)
        admin?   (r/atom false)
        class    (r/atom nil)]
    (fn []
      [modal/standard {:id "add-user-modal"}
       [:div.modal-content
        [:h4.center.purple-text.text-lighten-3
         (if (:admin @(rf/subscribe [:user])) "Add Teacher" "Add Student")]
        [:div.row
         [:div.col.s12
          [input/text
           {:placeholder "Email"
            :atom        email}]]
         [:div.col.m6.s12
          [input/text
           {:placeholder "Name"
            :atom        name}]]

         (if (:admin @(rf/subscribe [:user]))
           [:div
            [:div.input-field.col.m3.s6
             [input/checkbox
              {:label "Teacher"
               :atom  teacher?}]]
            [:div.input-field.col.m3.s6
             [input/checkbox
              {:label "Admin"
               :atom  admin?}]]]
           [:div.input-field.col.m6.s12
            [input/select
             {:placeholder "Class"
              :options     (get-years (js/Date.))
              :atom        class}]])]]
       [:div.modal-footer
        [:button.btn-flat.amber-text.darken-1.waves-effect.waves-purple
         {:on-click (fn []
                      (rf/dispatch [:user/post-user {:name    @name
                                                     :email   @email
                                                     :teacher @teacher?
                                                     :admin   @admin?
                                                     :class   @class}]))}
         "Submit"]]])))

(defn fab []
  (when (and
         (some #(% @(rf/subscribe [:user])) [:teacher :admin])
         (not (str/blank? @(rf/subscribe [:token]))))
    [:div {:style {:z-index  1
                   :position :fixed
                   :right    24
                   :bottom   24}}
     [:a.btn-floating.btn-large.amber.hoverable.modal-trigger
      {:href "#add-user-modal"}
      [:i.large.material-icons "add"]]]))


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
  (let [token       (rf/subscribe [:token])
        users       (rf/subscribe [:users])
        date        @(rf/subscribe [:date])
        students-uf (->> @users
                         (sort-by :name)
                         (sort-by #(user/search
                                    @(rf/subscribe [:search]) %)))
        students    (if (:admin @(rf/subscribe [:user]))
                      students-uf
                      (->> students-uf
                           (remove :teacher)
                           (remove :admin)))]
    [:div.container
     [search/search-bar]
     [search/date-bar]
     (if-not (seq @users)
       ;; Get users if empty.
       (rf/dispatch [:user/get])
       [:div.row
        [grid/grid
         (doall
          (if (nil? date)
            (map card students)
            (map card
                 (->> students
                      (remove #(user/flexblock-on-date? % date))
                      (remove :admin)))))]
        (doall (map modal students))
        (doall (map password/modal students))])]))

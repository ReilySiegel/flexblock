(ns flexblock.components.students
  (:require
   [clojure.string :as str]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [flexblock.rooms :as rooms]
   [flexblock.users :as user]
   [flexblock.utils :as u]
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

(defn get-years [date]
  (let [year  (.getFullYear date)
        month (.getMonth date)]
    (if (>= month 7)
      (range (inc year) (+ 5 year))
      (range year (+ 4 year)))))

(defn add-user-form
  "The form for creating a new user."
  []
  (r/create-class
   {:component-did-mount
    (fn []
      (.init js/M.CharacterCounter
             (.querySelectorAll js/document ".charcount"))
      (.init js/M.Select
             (.querySelectorAll js/document "select")))
    :reagent-render
    (fn []
      [:div.row
       [:div.col.s12
        [input/text
         {:placeholder   "Email"
          :class-name    "user-form"
          :dispatch-key  :add-user/set-email
          :subscribe-key :add-user/email}]]
       [:div.col.l6.m12
        [input/text
         {:placeholder   "Name"
          :class-name    "user-form"
          :dispatch-key  :add-user/set-name
          :subscribe-key :add-user/name}]]

       (if (:admin @(rf/subscribe [:user]))
         [:div
          [:div.input-field.col.l3.m6
           [:label
            [:input {:type      :checkbox
                     :on-change #(rf/dispatch [:add-user/set-teacher
                                               (-> %
                                                   .-target
                                                   .-checked)])}]
            [:span "Teacher"]]]
          [:div.input-field.col.l3.m6
           [:label
            [:input {:type      :checkbox
                     :on-change #(rf/dispatch [:add-user/set-admin
                                               (-> %
                                                   .-target
                                                   .-checked)])}]
            [:span "Admin"]]]]
         [:div.input-field.col.l6.m12
          (into [:select
                 {:on-change     #(rf/dispatch
                                   [:add-user/set-class
                                    (-> %
                                        .-target
                                        .-value
                                        js/parseInt)])
                  :default-value ""
                  :class-name    "user-form"}]
                (conj (for [year (get-years (js/Date.))]
                        ^{:key year} [:option {:value year} (str year)])
                      [:option {:value    ""
                                :disabled true} "Class"]))])])}))

(defn add
  "The modal that contains `flexblock.components.room/form`."
  []
  [modal/standard "add-user-modal"
   [:div.modal-content
    [:h4.center.purple-text.text-lighten-3
     (if (:admin @(rf/subscribe [:user])) "Add Teacher" "Add Student")]
    [add-user-form]]
   [:div.modal-footer
    [:a.btn-flat.amber-text.darken-1.waves-effect.waves-purple
     {:on-click #(rf/dispatch [:user/post-user])}
     "Submit"]]])

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
  (let [users (rf/subscribe [:users])]
    (if (seq @users)
      (let [date        @(rf/subscribe [:date])
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
          (doall (map password/modal students))]])
      [:div.grid-user])))

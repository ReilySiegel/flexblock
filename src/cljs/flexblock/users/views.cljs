(ns flexblock.users.views
  "Contains functions for rendering various parts of the users page."
  (:require
   [clojure.string :as str]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [flexblock.rooms :as rooms]
   [flexblock.users :as users]
   [flexblock.components.grid :as grid]
   [flexblock.search.views :as search]
   [flexblock.components.modal :as modal]
   [flexblock.components.input :as input]
   [flexblock.reminder.views :as reminder]
   [goog.string :as gstring]
   [goog.string.format]))


(defn password-modal
  "Shows a modal that allows a teacher to change the password of `user`."
  [user opts]
  (let [{:keys [id]
         :or   {id (str "passwordmodal" (:id user))}} opts
        password                                      (r/atom "")]
    ^{:key (:id user)}
    [modal/standard
     {:id       id
      :on-close #(reset! password "")}
     [:div.modal-content
      [:h4.center.purple-text.text-lighten-3
       (str "Reset Password for " (:name user))]
      [:div.row [:div.col.l6.offset-l3.m12
                 [input/text
                  {:type        :password
                   :placeholder "New Password"
                   :atom        password}]]]]
     [:div.modal-footer
      [:button.btn-flat.amber-text.waves-effect.waves-purple
       {:on-click #(rf/dispatch [:users/reset-password (:id user) @password])}
       "Reset Password"]]]))


(defn session
  "One session in :rooms list."
  [user room]
  [:li.collection-item
   {:key (:id room)}
   [:div
    {:style {:color (case @(rf/subscribe [:user/get-attendance
                                          (:id room)
                                          (:id user)])
                      -1 :red
                      1  :green
                      nil)}}
    (gstring/format "%s: %s - %s - %s %s "
                    (:title room)
                    (:name (rooms/get-teacher room))
                    (rooms/room-number-str room)
                    (rooms/time-str room)
                    (.toDateString (:date room)))]])

(defn sessions-modal
  "The bottom sheet modal that shows a list of all sessions a user is
  enrolled in."
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
           (doall (map (partial session user)
                       (->> sessions
                            (sort-by :date)
                            reverse)))]
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
  (let [user     (rf/subscribe [:login/user])
        name     (r/atom "")
        email    (r/atom "")
        teacher? (r/atom false)
        admin?   (r/atom false)
        class    (r/atom nil)]
    (fn []
      [modal/standard
       {:id       "add-user-modal"
        :on-close (fn []
                    (reset! name "")
                    (reset! email "")
                    ;; Possibly useful to keep teacher?, admin?, and
                    ;; class, as these are not likely to change when
                    ;; adding several users in a row. Open to change.
                    #_(reset! teacher? false?)
                    #_(reset! admin? false?)
                    #_(reset class nil))}
       [:div.modal-content
        [:h4.center.purple-text.text-lighten-3
         (if (:admin @user)
           "Add User"
           "Add Student")]
        [:div.row
         [:div.col.s12
          [input/text
           {:placeholder "Email"
            :atom        email}]]
         [:div.col.m6.s12
          [input/text
           {:placeholder "Name"
            :atom        name}]]

         (if (:admin @user)
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
                      (rf/dispatch [:users/post-user {:name    @name
                                                      :email   @email
                                                      :teacher @teacher?
                                                      :admin   @admin?
                                                      :class   @class}]))}
         "Submit"]]])))

(defn add-user-fab []
  (when (and
         (some #(% @(rf/subscribe [:login/user])) [:teacher :admin])
         (not (str/blank? @(rf/subscribe [:login/token]))))
    [:div {:style {:z-index  1
                   :position :fixed
                   :right    24
                   :bottom   24}}
     [:a.btn-floating.btn-large.amber.hoverable.modal-trigger
      {:href "#add-user-modal"}
      [:i.large.material-icons "add"]]]))


(defn- card-buttons
  "Returns the appropriate actions that a user can take on a `user`."
  [user]
  (let [self (rf/subscribe [:login/user])
        {:keys [id name rooms]}
        user]
    [:div.card-action
     [:a.btn-flat.amber-text.waves-effect.waves-purple.modal-trigger
      {:href (str "#sessionmodal" (:id user))} "Sessions"]
     [:a.btn-flat.amber-text.waves-effect.waves-purple.modal-trigger
      {:href (str "#passwordmodal" (:id user))} "Reset Password"]
     (when (users/can-delete? @self user)
       [:a.btn-flat.amber-text.waves-effect.waves-purple
        {:on-click #(rf/dispatch [:users/delete id])}
        "Delete"])]))


(defn card
  "Creates a card with information about a `user`."
  [user]
  (when-let [{:keys [id email name advisor rooms]} user]
    [:div.col.s12.m6.l4.grid-item
     {:key id}
     [:div.card.hoverable
      [:div.card-content
       [:span.card-title.truncate name]
       [:span.truncate email]
       (when advisor
         [:p.truncate advisor])]
      [:div.divider]
      [card-buttons user]
      [sessions-modal user]
      [password-modal user]]]))


(defn grid
  "Shows a grid of user cards."
  []
  (let [users (rf/subscribe [:users/sorted])]
    (if-not (seq @users)
      ;; Get users if empty.
      (rf/dispatch [:users/get])
      [:div.row
       [grid/grid
        (doall (map card @users))]])))


(defn page
  "Root component for the Users page."
  []
  [:div.container
   [search/search-bar]
   [search/date-bar]
   [grid]
   [reminder/modal]
   [add-user-fab]
   [add]])

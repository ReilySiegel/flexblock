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

(defn reset-password []
  (let [user @(rf/subscribe [:users/password-modal])
        {:keys [score score% feedback]}
        @(rf/subscribe [:users/password-strength])]
    [:div
     [:div.modal-content
      [:h4.center.purple-text.text-lighten-3
       (str "Reset Password for " (:name user))]
      [:div.row [:div.col.l6.offset-l3.s12
                 [input/text
                  {:type          :password
                   :placeholder   "New Password"
                   :dispatch-key  :users/set-password
                   :subscribe-key :users/password}]]]
      [:div.row
       [:div.col.l6.s12.offset-l3.center
        [:div.progress
         [:div.determinate
          {:style {:width score%}
           :class (cond
                    (< 80 score) "green"
                    (< 60 score) "yellow"
                    :else        "red")}]]
        [:span (:warning feedback)]]]]
     [:div.modal-footer
      [:button.btn-flat.amber-text.waves-effect.waves-purple
       {:on-click #(rf/dispatch
                    [:users/reset-password (:id user)])}
       "Reset Password"]]]))

(defn password-modal
  "Shows a modal that allows a teacher to change the password of `user`."
  []
  (let [password (r/atom "")]
    [modal/standard
     {:id       "password-modal"
      :on-close #(rf/dispatch [:users/set-password ""])}
     [reset-password]]))


(defn session
  "One session in :rooms list."
  [user room]
  [:li.collection-item
   {:key (:id room)}
   [:div
    {:style {:color (case @(rf/subscribe [:room/get-attendance
                                          (:id room)
                                          (:id user)])
                      -1 :red
                      1  :green
                      nil)}}
    (gstring/format "%s: %s - %s %s"
                    (rooms/room-number-str room)
                    (:title room)
                    (rooms/time-str room)
                    (.toDateString (:date room)))]])

(defn sessions []
  (let [user     @(rf/subscribe [:users/session-modal])
        sessions (:rooms user)]
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
          (str (:name user) " is not enrolled in any Sessions.")])]]]))

(defn sessions-modal
  "The bottom sheet modal that shows a list of all sessions a user is
  enrolled in."
  []
  [modal/bottom-sheet {:id "session-modal"}
   [sessions]])

(defn get-years [date]
  (let [year  (.getFullYear date)
        month (.getMonth date)]
    (if (>= month 7)
      (range (inc year) (+ 5 year))
      (range year (+ 4 year)))))

(defn add-student
  "the form for adding a student. `name`, `email`, and `class` should be
  atoms."
  [name email class]
  [:div.modal-content
   [:h4.center.purple-text.text-lighten-3 "Add Student"]
   [:div.row
    [:div.col.s12
     [input/text
      {:placeholder "Email"
       :atom        email}]]
    [:div.col.m6.s12
     [input/text
      {:placeholder "Name"
       :atom        name}]]
    [:div.input-field.col.m6.s12
     [input/select
      {:placeholder "Class"
       :options     (get-years (js/Date.))
       :atom        class}]]]])

(defn add-staff [name email admin? teacher?]
  [:div.modal-content
   [:h4.center.purple-text.text-lighten-3  "Add User"]
   [:div.row
    [:div.col.s12
     [input/text
      {:placeholder "Email"
       :atom        email}]]
    [:div.col.m6.s12
     [input/text
      {:placeholder "Name"
       :atom        name}]]
    [:div
     [:div.input-field.col.m3.s6
      [input/checkbox
       {:label "Teacher"
        :atom  teacher?}]]
     [:div.input-field.col.m3.s6
      [input/checkbox
       {:label "Admin"
        :atom  admin?}]]]]])

(defn add
  "The form for creating a new user."
  []
  (let [user     (rf/subscribe [:login/user])
        name     (r/atom "")
        email    (r/atom "")
        teacher? (r/atom false)
        admin?   (r/atom false)
        class    (r/atom nil)
        reset-fn (fn []
                   (reset! name "")
                   (reset! email "")
                   (reset! teacher? false)
                   (reset! admin? false)
                   (reset! class nil))]
    (r/create-class
     {:component-did-mount
      #(let [e (.getElementById js/document "tabs")]
         (when e
           (.init js/M.Tabs e (clj->js {:onShow reset-fn}))))
      :reagent-render
      (fn []
        [modal/standard
         {:id       "add-user-modal"
          :on-close reset-fn}
         (if-not (:admin @user)
           [add-student name email class]
           [:div.row
            [:div.col.s12
             [:ul.tabs
              {:id "tabs"}
              [:li.tab.col.s6 [:a {:href "#staff"} "Add User"]]
              [:li.tab.col.s6 [:a {:href "#student"} "Add Student"]]]]
            [:div.col.s12 {:id "staff"} [add-staff name email admin? teacher?]]
            [:div.col.s12 {:id "student"} [add-student name email class]]])
         [:div.modal-footer
          [:button.btn-flat.amber-text.darken-1.waves-effect.waves-purple
           {:on-click (fn []
                        (rf/dispatch [:users/post-user {:name    @name
                                                        :email   @email
                                                        :teacher @teacher?
                                                        :admin   @admin?
                                                        :class   @class}]))}
           "Submit"]]])})))

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
     [:a.btn-flat.amber-text.waves-effect.waves-purple
      {:on-click #(rf/dispatch [:users/set-session-modal user])}
      "Sessions"]
     [:a.btn-flat.amber-text.waves-effect.waves-purple
      {:on-click #(rf/dispatch [:users/set-password-modal user])}
      "Reset Password"]
     (when (and (users/can-edit? @self user)
                (:teacher @self)
                (= :student (users/highest-role user))
                (nil? (:advisor-id user)))
       [:a.btn-flat.amber-text.waves-effect.waves-purple
        {:on-click #(rf/dispatch [:users/claim id])}
        "Claim"])
     (when (and (or (= (:advisor-id user) (:id self))
                    (:admin @self))
                (= :student (users/highest-role user))
                (:advisor-id user))
       [:a.btn-flat.amber-text.waves-effect.waves-purple
        {:on-click #(rf/dispatch [:users/abandon id])}
        "Reset Advisor"])
     (when (users/can-delete? @self user)
       [:a.btn-flat.amber-text.waves-effect.waves-purple
        {:on-click #(rf/dispatch [:users/delete id])}
        "Delete"])]))


(defn card
  "Creates a card with information about a `user`."
  [user]
  (when-let [{:keys [id email name advisor-name rooms]} user]
    [:div.col.s12.m6.l4.grid-item
     {:key id}
     [:div.card.hoverable
      [:div.card-content
       [:span.card-title.truncate name]
       [:span.truncate email]
       (when advisor-name
         [:p.truncate advisor-name])]
      [:div.divider]
      [card-buttons user]]]))


(defn grid
  "Shows a grid of user cards."
  []
  (let [users (rf/subscribe [:users/sorted])]
    (if-not (seq @users)
      ;; Get users if empty.
      (rf/dispatch [:users/get])
      [:div.row
       [grid/grid
        (doall (map card (take 36 @users)))]])))

(defn filters []
  (let [filters @(rf/subscribe [:users/role-filter])
        show?   @(rf/subscribe [:users/filter])]
    [:div
     (when show?
       [:div
        [search/date-bar]
        [:div.row
         (for [k (keys users/roles)]
           [:div.col.s4.m3.offset-m1
            {:key k}
            [:label
             [:input
              {:type      :checkbox
               :checked   (contains? filters k)
               :value     (filters k)
               :on-change #(rf/dispatch [:users/update-role-filter
                                         k
                                         (-> %
                                             .-target
                                             .-checked)])}]
             [:span (str/capitalize (name k))]]])]])
     [:div.row
      [:div.col.s12.center
       {:style {:padding-top (if show? "2vh" "0px")}}
       [:a
        {:style    {:cursor :pointer}
         :on-click #(rf/dispatch [:users/toggle-filter])}
        (if show? "Hide Filters" "Show Filters")]]]]))


(defn page
  "Root component for the Users page."
  []
  [:div.container
   [search/search-bar]
   [filters]
   [grid]
   [reminder/modal]
   [add-user-fab]
   [add]
   [sessions-modal]
   [password-modal]])

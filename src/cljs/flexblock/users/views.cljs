(ns flexblock.users.views
  "Contains functions for rendering various parts of the users page."
  (:require
   [clojure.string :as str]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [flexblock.rooms :as rooms]
   [flexblock.users :as users]
   [flexblock.search.views :as search]
   [flexblock.components.material :as material]
   [flexblock.interop :as interop]
   [flexblock.reminder.views :as reminder]
   [goog.string :as gstring]
   [goog.string.format]))

(defn reset-password [user]
  (let [{:keys [score feedback]}
        @(rf/subscribe [:users/password-strength])
        color (if (< 70 score)
                :primary
                :secondary)]
    [:div
     [material/DialogTitle
      (str "Reset Password for " (:name user))]
     [material/DialogContent
      [material/Grid
       {:container true
        :justify   :center}
       [material/Grid
        {:item true
         :xs   12
         :md   6}
        [material/TextField
         {:type        :password
          :autoFocus   true
          :fullWidth   true
          :label       "New Password"
          :placeholder "correcthorsebatterystaple"
          :value       @(rf/subscribe [:users/password])
          :onChange    #(rf/dispatch [:users/set-password
                                      (interop/event->value %)])}]]]
      [material/Grid
       {:container true
        :justify   :center}
       [material/Grid
        {:item true
         :xs   12
         :md   6}
        [material/LinearProgress
         {:variant :determinate
          :value   score
          :color   color}]
        [material/Typography (:warning feedback)]]]]
     [material/DialogActions
      [material/Button
       {:color   :secondary
        :onClick (fn []
                   (rf/dispatch-sync
                    [:users/reset-password (:id user)])
                   (rf/dispatch [:users/set-password ""]))}
       "Reset Password"]]]))

(defn password-modal
  "Shows a modal that allows a teacher to change the password of `user`."
  []
  (let [user @(rf/subscribe [:users/password-modal])]
    [material/Dialog
     {:open      (boolean user)
      :fullWidth true
      :onClose   (fn []
                   (rf/dispatch [:users/set-password-modal nil])
                   (rf/dispatch [:users/set-password ""]))}
     [reset-password user]]))


(defn session
  "One session in :rooms list."
  [user room]
  (let [attendance     @(rf/subscribe [:room/get-attendance
                                       (:id room)
                                       (:id user)])
        [avatar label] (get rooms/attendance->icon attendance)]
    [material/ListItem
     {:key (:id room)}
     [material/ListItemAvatar
      [material/Avatar
       [:i.material-icons avatar]]]
     [material/ListItemText
      {:primary   (:title room)
       :secondary (gstring/format "%s: %s %s"
                                  (rooms/room-number-str room)
                                  (rooms/time-str room)
                                  (.toDateString (:date room)))}]]))

(defn sessions [user]
  (let [sessions (:rooms user)]
    [material/DialogContent
     [material/Typography
      {:variant :h5}
      "Sessions"]
     [material/Grid
      {:container true
       :justify   :center}
      [material/Grid
       {:item true
        :xs   12
        :lg   8}
       (if (seq sessions)
         [:ul.collection
          (doall (map (partial session user)
                      (->> sessions
                           (sort-by :date)
                           reverse)))]
         [material/Typography
          (str (:name user) " is not enrolled in any Sessions.")])]]]))

(defn sessions-modal
  "The bottom sheet modal that shows a list of all sessions a user is
  enrolled in."
  []
  (let [user @(rf/subscribe [:users/session-modal])]
    [material/Drawer
     {:anchor  :bottom
      :open    (boolean user)
      :onClose #(rf/dispatch [:users/set-session-modal nil])}
     [sessions user]]))

(defn get-years [date]
  (let [year  (.getFullYear date)
        month (.getMonth date)]
    (if (>= month 7)
      (range (inc year) (+ 5 year))
      (range year (+ 4 year)))))

(defn add-student [name email class]
  [material/Grid
   {:container true
    :spacing   16}
   [material/Grid
    {:item true :xs 12}
    [material/TextField
     {:label       "Email"
      :placeholder "20xx@ellingtonschools.net"
      :fullWidth   true
      :value       @email
      :onChange    #(reset! email (-> % .-target .-value))}]]
   [material/Grid
    {:item true :xs 12 :sm 6}
    [material/TextField
     {:label       "Name"
      :placeholder "Joe Average"
      :fullWidth   true
      :value       @name
      :onChange    #(reset! name (-> % .-target .-value))}]]
   [material/Grid {:item true :xs 12 :sm 6}
    [material/TextField
     {:label     "Class"
      :fullWidth true
      :select    true
      :value     @class
      :onChange  #(reset! class
                          (-> % .-target .-value))}
     (for [year (get-years (js/Date.))]
       [material/MenuItem
        {:key   year
         :value (str year)}
        (str year)])]]])

(defn add-staff [name email admin? teacher?]
  [material/Grid
   {:container true
    :spacing   16}
   [material/Grid
    {:item true :xs 12}
    [material/TextField
     {:label       "Email"
      :placeholder "averagej@ellingtonschools.net"
      :fullWidth   true
      :value       @email
      :onChange    #(reset! email (-> % .-target .-value))}]]
   [material/Grid
    {:item true :xs 12 :sm 6}
    [material/TextField
     {:label       "Name"
      :placeholder "Joe Average"
      :fullWidth   true
      :value       @name
      :onChange    #(reset! name (-> % .-target .-value))}]]
   [material/Grid {:item true :xs 6 :sm 3}
    [material/FormControlLabel
     {:label "Teacher"
      :control
      (r/as-element
       [material/Checkbox
        {:checked  @teacher?
         :onChange #(reset! teacher? (-> %
                                         .-target
                                         .-checked))}])}]]
   [material/Grid {:item true :xs 6 :sm 3}
    [material/FormControlLabel
     {:label "Admin"
      :control
      (r/as-element
       [material/Checkbox
        {:checked  @admin?
         :onChange #(reset! admin? (-> %
                                       .-target
                                       .-checked))}])}]]])

(defn add
  "The form for creating a new user."
  []
  (let [user     (rf/subscribe [:login/user])
        name     (r/atom "")
        email    (r/atom "")
        teacher? (r/atom false)
        admin?   (r/atom false)
        class    (r/atom "")
        tab      (r/atom 0)
        reset-fn (fn []
                   (reset! name "")
                   (reset! email "")
                   (reset! teacher? false)
                   (reset! admin? false)
                   (reset! class ""))]
    (fn []
      [material/Dialog
       {:open      @(rf/subscribe [:users/modal-open])
        :onClose   #(rf/dispatch [:users/set-modal-open false])
        :fullWidth true}
       [material/Tabs
        {:value          @tab
         :indicatorColor :primary
         :textColor      :primary
         :fullWidth      true
         :onChange       (fn [_ selected]
                           (reset-fn)
                           (reset! tab selected))}
        [material/Tab {:label "Add User"}]
        [material/Tab {:label "Add Student"}]]
       [material/DialogContent
        (condp = @tab
          0 [add-staff name email admin? teacher?]
          1 [add-student name email class])]
       [material/DialogActions
        [material/Button
         {:color :secondary
          :on-click
          (fn []
            (rf/dispatch [:users/post-user
                          {:name    @name
                           :email   @email
                           :teacher @teacher?
                           :admin   @admin?
                           :class   (.parseInt js/window
                                               @class)}]))}
         "Add User"]]])))

(defn add-user-fab []
  (when (and
         (some #(% @(rf/subscribe [:login/user])) [:teacher :admin])
         (not (str/blank? @(rf/subscribe [:login/token]))))
    [material/Zoom
     {:in true}
     [material/Button
      {:variant :fab
       :color   :primary
       :onClick #(rf/dispatch [:users/set-modal-open true])
       :style   {:position :fixed
                 :right    "2em"
                 :bottom   "2em"}}
      [:i.material-icons :add]]]))

(defn- card-buttons
  "Returns the appropriate actions that a user can take on a `user`."
  [user]
  (let [self (rf/subscribe [:login/user])
        {:keys [id name rooms]}
        user]
    [material/CardActions
     [material/Tooltip {:title "Sessions"}
      [material/IconButton
       {:on-click #(rf/dispatch [:users/set-session-modal user])}
       [:i.material-icons :list]]]
     [material/Tooltip {:title "Reset Password"}
      [material/IconButton
       {:onClick #(rf/dispatch [:users/set-password-modal user])}
       [:i.material-icons :lock]]]
     (when (and (users/can-edit? @self user)
                (:teacher @self)
                (= :student (users/highest-role user))
                (nil? (:advisor-id user)))
       [material/Tooltip {:title "Claim"}
        [material/IconButton
         {:onClick #(rf/dispatch [:users/claim id])}
         [:i.material-icons :group_add]]])
     (when (and (or (= (:advisor-id user) (:id self))
                    (:admin @self))
                (= :student (users/highest-role user))
                (:advisor-id user))
       [material/Tooltip {:title "Reset Advisor"}
        [material/IconButton
         {:onClick #(rf/dispatch [:users/abandon id])}
         [:i.material-icons :restore]]])
     (when (users/can-delete? @self user)
       [material/Tooltip {:title "Delete"}
        [material/IconButton
         {:onClick #(rf/dispatch [:users/delete id])}
         [:i.material-icons :delete]]])]))


(defn card
  "Creates a card with information about a `user`."
  [index
   {:keys [id email name advisor-name rooms]
    :as   user}]
  [material/Grid
   {:item true
    :xs   12
    :sm   6
    :lg   4
    :key  id
    :class
    (condp = (str/lower-case @(rf/subscribe [:search]))
      ;; Rotate the card by 2 degrees.
      "askew"            "askew"
      ;; Does a barrel roll.
      "do a barrel roll" "barrel-roll"
      "")}
   [material/Slide
    {:in        true
     :direction :right
     :timeout   (* 250 (inc index))}
    [material/Card
     [material/CardContent
      [material/Typography
       {:variant :h5
        :noWrap  true}
       name]
      [material/Typography
       {:variant :subtitle1
        :color   :textSecondary
        :noWrap  true}
       email]]
     [card-buttons user]]]])

(defn grid
  "Shows a grid of user cards."
  []
  (let [users (rf/subscribe [:users/sorted])]
    (if-not (seq @users)
      ;; Get users if empty.
      (rf/dispatch [:users/get])
      [material/Grid
       {:container true
        :spacing   16
        :style     {:padding-top "3em"}}
       (doall (map-indexed card (take 36 @users)))])))

(defn filters []
  (let [filters @(rf/subscribe [:users/role-filter])
        show?   @(rf/subscribe [:users/filter])]
    [material/Grid
     {:container true
      :item      true
      :lg        6
      :md        8
      :xs        10
      :justify   :center
      :style     {:padding-top "3vh"}}
     [material/Collapse
      {:in show?}
      [material/Grid
       {:container true
        :justify   :center}
       [search/date-bar]
       (for [k (keys users/roles)]
         [material/Grid
          {:item true
           :sm   4
           :xs   12
           :key  k}
          [material/FormControlLabel
           {:label (str/capitalize (name k))
            :control
            (r/as-element
             [material/Checkbox
              {:checked  (boolean (filters k))
               :onChange #(rf/dispatch [:users/update-role-filter
                                        k
                                        (-> %
                                            .-target
                                            .-checked)])}])}]])]]
     [material/Grid
      {:container true
       :justify   :center
       :style     {:padding-top (if show? "2vh" "0px")}}
      [material/Button
       {:color   :inherit
        :onClick #(rf/dispatch [:users/toggle-filter])}
       (if show? "Hide Filters" "Show Filters")]]]))


(defn page
  "Root component for the Users page."
  []
  [material/Grid
   {:container true
    :justify   :center}
   [material/Grid
    {:container true
     :item      true
     :xl        8
     :xs        10
     :justify   :center}
    [search/search-bar]
    [filters]
    [grid]
    [reminder/modal]
    [add-user-fab]
    [add]
    [sessions-modal]]])

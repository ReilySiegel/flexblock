(ns flexblock.rooms.views
  "Render functions for elements related to showing rooms."
  (:require [cljs.reader :as reader]
            [clojure.set :as set]
            [clojure.string :as str]
            [flexblock.components.grid :as grid]
            [flexblock.components.material :as material]
            [flexblock.components.modal :as modal]
            [flexblock.interop :as interop]
            [flexblock.rooms :as rooms]
            [flexblock.users :as users]
            [flexblock.search.views :as search]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn add
  "The form for creating a new room."
  []
  (let [title       (r/atom "")
        number      (r/atom "")
        capacity    (r/atom "")
        description (r/atom "")
        date        (r/atom "")
        time        (r/atom "")
        open        (rf/subscribe [:rooms/modal-open])
        reset-fn    (fn []
                      (reset! title "")
                      (reset! capacity "")
                      (reset! number "")
                      (reset! description "")
                      (reset! date "")
                      (reset! time ""))]
    (fn []
      [material/Dialog
       {:open    @open
        :scroll  :body
        ;; Reset all inputs when when the modal is closed.
        :onClose (fn []
                   (rf/dispatch [:rooms/set-modal-open false])
                   (reset-fn))}
       [material/DialogTitle "Add Session"]
       [material/DialogContent
        [material/Grid {:container true :spacing 16}
         [material/Grid {:item true :xs 12}
          [material/TextField
           {:label     "Title"
            :autoFocus true
            :fullWidth true
            :value     @title
            :onChange  #(reset! title (-> % .-target .-value))}]]
         [material/Grid {:item true :xs 12 :sm 6}
          [material/TextField
           {:label     "Room Number"
            :fullWidth true
            :value     @number
            :onChange  #(reset! number (-> % .-target .-value))}]]
         [material/Grid {:item true :xs 12 :sm 6}
          [material/TextField
           {:label     "Max Capacity"
            :fullWidth true
            :value     @capacity
            :type      :number
            :onChange  #(reset! capacity (-> % .-target .-value))}]]
         [material/Grid {:item true :xs 12}
          [material/TextField
           {:label     "Description"
            :fullWidth true
            :value     @description
            :onChange  #(reset! description (-> % .-target .-value))}]]
         [material/Grid {:item true :xs 12 :sm 6}
          [material/TextField
           {:label           "Date"
            :fullWidth       true
            :type            :date
            :value           @date
            :onChange        #(reset! date (-> % .-target .-value))
            :InputLabelProps {:shrink true}}]]
         [material/Grid {:item true :xs 12 :sm 6}
          [material/TextField
           {:label     "Time"
            :fullWidth true
            :select    true
            :value     @time
            :onChange  #(reset! time
                                (-> % .-target .-value))}
           (for [[time label] rooms/sorted-times]
             [material/MenuItem
              {:key   time
               :value label}
              label])]]]]
       [material/DialogActions
        [material/Button
         {:color   :secondary
          :onClick (fn []
                     (rf/dispatch
                      [:room/post {:title        @title
                                   :max-capacity @capacity
                                   :room-number  @number
                                   :description  @description
                                   :date
                                   (interop/str->date @date)
                                   :time
                                   (get (set/map-invert rooms/times)
                                        @time)}])
                     (reset-fn))}
         "Submit"]]])))

(defn student
  "One student in the attendance list."
  [room user]
  (let [attendance     @(rf/subscribe [:room/get-attendance
                                       (:id room)
                                       (:id user)])
        [avatar label] (get rooms/attendance->icon attendance)]
    [material/ListItem
     {:key (:id user)}
     [material/ListItemAvatar
      [material/Avatar
       [:i.material-icons avatar]]]
     [material/ListItemText
      {:primary   (:name user)
       :secondary label}]
     [material/ListItemSecondaryAction
      [material/Tooltip {:title "Present"}
       [material/IconButton
        {:onClick #(rf/dispatch [:room/set-attendance
                                 (:id room)
                                 (:id user)
                                 1])}
        [:i.material-icons :check]]]
      [material/Tooltip {:title "Late"}
       [material/IconButton
        {:onClick #(rf/dispatch [:room/set-attendance
                                 (:id room)
                                 (:id user)
                                 -2])}
        [:i.material-icons :priority_high]]]
      [material/Tooltip {:title "Absent"}
       [material/IconButton
        {:onClick #(rf/dispatch [:room/set-attendance
                                 (:id room)
                                 (:id user)
                                 -1])}
        [:i.material-icons :clear]]]]]))

(defn attendance []
  (let [room            @(rf/subscribe [:rooms/attendance-modal])
        students        (rooms/get-students room)
        students-sorted (->> students
                             (sort-by :name)
                             (sort-by :attendance)
                             (sort-by (fn [student]
                                        (not (zero?
                                              (:attendance student))))))]
    [material/Grid
     {:container true
      :justify   :center}
     [material/Grid
      {:item true
       :xs   12
       :lg   8}
      (if (seq students)
        [material/List
         (doall (map (partial student room) students-sorted))]
        [material/Typography
         "No students have joined yet."])]]))

(defn attendance-modal
  "The bottom drawer that shows a list of students."
  []
  [material/Drawer
   {:open    (boolean @(rf/subscribe [:rooms/attendance-modal]))
    :anchor  :bottom
    :onClose #(rf/dispatch [:rooms/set-attendance-modal nil])}
   [material/DialogContent
    [material/Typography {:variant :h5} "Students"]
    [attendance]]])

(defn buttons
  "Returns the appropriate actions that a user can take on a `room`."
  [room expand?]
  (let [{:keys [id title users description date room-number max-capacity]}
        room
        user          @(rf/subscribe [:login/user])
        user-in-room? ((->> users (map :id) (apply hash-set)) (:id user))]
    [material/CardActions
     ;; Join and Leave room.
     (cond
       (and (not (:teacher user))
            (not user-in-room?))
       [material/Tooltip {:title "Join"}
        [material/IconButton
         {:on-click #(rf/dispatch [:room/join id])}
         [:i.material-icons :person_add]]]

       (and (not (:teacher user))
            user-in-room?)
       [material/Tooltip {:title "Leave"}
        [material/IconButton
         {:on-click #(rf/dispatch [:room/leave id])}
         [:i.material-icons :person_add_disabled]]])
     (when-not (= :student (users/highest-role user))
       [material/Tooltip {:title "Students"}
        [material/IconButton
         {:on-click #(rf/dispatch [:rooms/set-attendance-modal room])}
         [:i.material-icons :list]]])
     (when (or (:admin user)
               (and user-in-room? (:teacher user)))
       [material/Tooltip {:title "Delete"}
        [material/IconButton
         {:on-click #(rf/dispatch [:room/delete id])}
         [:i.material-icons :delete]]])
     [material/Tooltip {:title "Description"}
      [material/IconButton
       {:style    {:margin-left :auto}
        :on-click #(swap! expand? not)}
       [:i.material-icons (if @expand?
                            :expand_less
                            :expand_more)]]]]))

(defn date-string [date]
  (-> date
      (.toUTCString)
      ;; Remove time data.
      (str/split #"\d\d:\d\d")
      first
      ;; Remove comma.
      (str/replace #"," "")))

(defn card
  "Creates a card with information about a `room`."
  [index room]
  (let [expand? (r/atom false)]
    (fn [index
         {:keys [id title users description date time room-number max-capacity]
          :as   room}]
      [material/Grid
       {:item true
        :xs   12
        :sm   6
        :md   4
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
           title]
          [material/Typography
           {:variant :body1
            :color   :textSecondary
            :noWrap  true}
           (rooms/time-str room)]
          [material/Typography
           {:variant :body1
            :color   :textSecondary
            :noWrap  true}
           (date-string date)]
          [material/Typography
           {:variant :body1
            :color   :textSecondary
            :noWrap  true}
           (rooms/room-number-str room)]
          [material/Typography
           {:variant :body1
            :color   :textSecondary
            :noWrap  true}
           (:name (rooms/get-teacher room))]
          [material/Typography
           {:variant :body1
            :color   :textSecondary
            :noWrap  true}
           (str (dec (count users)) "/" (:max-capacity room))]]
         [buttons room expand?]
         [material/Collapse
          {:in @expand?}
          [material/CardContent
           [material/Typography
            description]]]]]])))

(defn fab []
  (when (and
         (:teacher @(rf/subscribe [:login/user]))
         (not (str/blank? @(rf/subscribe [:login/token]))))
    [material/Zoom
     {:in true}
     [material/Button
      {:variant :fab
       :color   :primary
       :onClick #(rf/dispatch [:rooms/set-modal-open true])
       :style   {:position :fixed
                 :right    "2em"
                 :bottom   "2em"}}
      [:i.material-icons :add]]]))

(defn grid
  "Returns a grid of rooms."
  []
  (let [token (rf/subscribe [:login/token])
        rooms (rf/subscribe [:rooms/sorted])]
    (when-not (empty? @token)
      (if-not (seq @rooms)
        ;; Get rooms if rooms are empty.
        (rf/dispatch [:rooms/get])
        ;; Otherwise show the rooms
        (into [material/Grid
               {:container true
                :spacing   16
                :style     {:padding-top "3em"}}]
              (doall (map-indexed (fn [i r]
                                    [card i r]) @rooms)))))))


(defn filters []
  (let [filters @(rf/subscribe [:rooms/time-filter])
        show?   @(rf/subscribe [:rooms/filter])]
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
       {:container true}
       (for [[k s] rooms/sorted-times]
         [material/Grid
          {:item true
           :sm   3
           :xs   6
           :key  k}
          [material/FormControlLabel
           {:label s
            :control
            (r/as-element
             [material/Checkbox
              {:checked  (boolean (filters k))
               :onChange #(rf/dispatch [:rooms/update-time-filter
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
        :onClick #(rf/dispatch [:rooms/toggle-filter])}
       (if show? "Hide Filters" "Show Filters")]]]))


(defn page
  "Root component for the rooms page.
  Consists of a search bar, a grid of rooms, and a FAB that can be
  used to open a form for adding rooms."
  []
  (when-not (str/blank? @(rf/subscribe [:login/token]))
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
      [add]
      [fab]
      [grid]
      [attendance-modal]]]))

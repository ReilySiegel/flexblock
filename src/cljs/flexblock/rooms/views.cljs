(ns flexblock.rooms.views
  "Render functions for elements related to showing rooms."
  (:require [cljs.reader :as reader]
            [clojure.set :as set]
            [clojure.string :as str]
            [flexblock.components.material :as material]
            [flexblock.interop :as interop]
            [flexblock.rooms :as rooms]
            [flexblock.users :as users]
            [flexblock.search.views :as search]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn add
  "The form for creating a new room."
  []
  (let [title       (rf/subscribe [:rooms.form/title])
        number      (rf/subscribe [:rooms.form/number])
        capacity    (rf/subscribe [:rooms.form/capacity])
        description (rf/subscribe [:rooms.form/description])
        date        (rf/subscribe [:rooms.form/date])
        time        (rf/subscribe [:rooms.form/time])
        open        (rf/subscribe [:rooms/modal-open])
        reset-fn    (fn []
                      (rf/dispatch [:rooms.form/set-title ""])
                      (rf/dispatch [:rooms.form/set-number ""])
                      (rf/dispatch [:rooms.form/set-capacity ""])
                      (rf/dispatch [:rooms.form/set-description ""])
                      (rf/dispatch [:rooms.form/set-date ""])
                      (rf/dispatch [:rooms.form/set-time []]))]
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
        [material/Grid {:container true :spacing 16
                        :style     {:align-items :flex-end}}
         [material/Grid {:item true :xs 12}
          [material/TextField
           {:label     "Title"
            :autoFocus true
            :fullWidth true
            :value     @title
            :onChange  #(rf/dispatch [:rooms.form/set-title
                                      (interop/event->value %)])}]]
         [material/Grid {:item true :xs 12 :sm 6}
          [material/TextField
           {:label     "Room Number"
            :fullWidth true
            :value     @number
            :onChange  #(rf/dispatch [:rooms.form/set-number
                                      (interop/event->value %)])}]]
         [material/Grid {:item true :xs 12 :sm 6}
          [material/TextField
           {:label     "Max Capacity"
            :fullWidth true
            :value     @capacity
            :type      :number
            :onChange  #(rf/dispatch [:rooms.form/set-capacity
                                      (interop/event->value %)])}]]
         [material/Grid {:item true :xs 12}
          [material/TextField
           {:label     "Description"
            :fullWidth true
            :value     @description
            :onChange  #(rf/dispatch [:rooms.form/set-description
                                      (interop/event->value %)])}]]
         [material/Grid {:item true :xs 12 :sm 6}
          [material/TextField
           {:label           "Date"
            :fullWidth       true
            :type            :date
            :value           @date
            :onChange        #(rf/dispatch [:rooms.form/set-date
                                            (interop/event->value %)])
            :InputLabelProps {:shrink true}}]]
         [material/Grid {:item true :xs 12 :sm 6}
          [material/TextField
           {:label       "Time"
            :fullWidth   true
            :select      true
            :SelectProps {:multiple true
                          :renderValue
                          (fn [selected]
                            (r/as-element
                             [:div
                              {:style {:display  :flex
                                       :flexWrap :wrap}}
                              (for [time (->> selected
                                              js->clj
                                              sort
                                              (sort-by count))]
                                [material/Chip
                                 {:key   time
                                  :value time
                                  :label time
                                  :style {:margin "0.25em"}}])]))}
            :value       @time
            :onChange    #(rf/dispatch [:rooms.form/set-time
                                        (interop/event->value %)])}
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
                                   (mapv #(get (set/map-invert rooms/times) %)
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
   {:open       (boolean @(rf/subscribe [:rooms/attendance-modal]))
    :anchor     :bottom
    :onClose    #(rf/dispatch [:rooms/set-attendance-modal nil])
    :PaperProps {:style {:max-height "50vh"}}}
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
        (condp = (str/lower-case @(rf/subscribe [:search-debounced]))
          ;; Rotate the card by 2 degrees.
          "askew"            "askew"
          ;; Does a barrel roll.
          "do a barrel roll" "barrel-roll"
          "")}
       [material/Slide
        {:in        true
         :direction :right
         :timeout   (* 250 (.sqrt js/Math (* 2 (inc index))))}
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
           (interop/date->str date)]
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
  (let [open (r/atom false)]
    (fn []
      (let [user  @(rf/subscribe [:login/user])
            rooms (:rooms user)]
        (when (and
               (:teacher user)
               (not (str/blank? @(rf/subscribe [:login/token]))))
          [material/Zoom
           {:in true}
           [material/SpeedDial
            {:ariaLabel    "Add Room"
             :open         @open
             :ButtonProps  {:color :secondary
                            :onClick
                            #(rf/dispatch [:rooms/set-modal-open true])}
             :icon         (r/as-element [material/Icon :add])
             :onMouseEnter #(reset! open true)
             :onMouseLeave #(reset! open false)
             :style        {:position :fixed
                            :right    "2em"
                            :bottom   "2em"}}
            (for [room (->> rooms
                            ;; Ensure that titles are unique, so that
                            ;; rooms with the same content in
                            ;; different blocks are not duplicated.
                            (group-by :title)
                            vals
                            (map first)
                            ;; Only show five items.
                            (take 5))]
              [material/SpeedDialAction
               {:key          (:id room)
                :icon         (r/as-element [material/Icon "file_copy"])
                :tooltipTitle (:title room)
                :ButtonProps
                {:onClick
                 (fn []
                   (rf/dispatch-sync [:rooms.form/set-title
                                      (:title room)])
                   (rf/dispatch-sync [:rooms.form/set-number
                                      (:room-number room)])
                   (rf/dispatch-sync [:rooms.form/set-capacity
                                      (:max-capacity room)])
                   (rf/dispatch-sync [:rooms.form/set-description
                                      (:description room)])
                   (rf/dispatch-sync [:rooms/set-modal-open true]))}}])]])))))

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
              (doall (take 36 (map-indexed (fn [i r]
                                             [card i r]) @rooms))))))))


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
      {:in            show?
       :mountOnEnter  true
       :unmountOnExit true}
      [material/Grid
       {:container true}
       (for [[k s] rooms/sorted-times]
         [material/Grid
          {:item  true
           :sm    3
           :xs    6
           :key   k
           :style {:padding-left "14px"}}
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

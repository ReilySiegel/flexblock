(ns flexblock.rooms.views
  "Render functions for elements related to showing rooms."
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [clojure.string :as str]
   [flexblock.rooms :as rooms]
   [flexblock.components.input :as input]
   [flexblock.components.grid :as grid]
   [flexblock.components.modal :as modal]
   [flexblock.search.views :as search])
  (:import goog.date.Date))

(defn add
  "The form for creating a new room."
  []
  (let [title       (r/atom "")
        number      (r/atom "")
        capacity    (r/atom "")
        description (r/atom "")
        date        (r/atom nil)
        time        (r/atom nil)]
    (fn []
      [modal/fixed-footer
       {:id       "add-room-modal"
        ;; Reset all inputs when when the modal is closed.
        :on-close (fn []
                    (reset! title "")
                    (reset! capacity "")
                    (reset! number "")
                    (reset! description "")
                    (reset! date nil)
                    (reset! time nil))}
       [:div.modal-content
        [:h4.center.purple-text.text-lighten-3 "Add Session"]
        [:div.row
         [:div.col.s12
          [input/text {:placeholder "Title" :atom title}]]
         [:div.col.m6.s12
          [input/text {:placeholder "Room Number"
                       :atom        number}]]
         [:div.col.m6.s12
          [input/text {:placeholder "Max Capacity"
                       :atom        capacity
                       :type        :number}]]
         [:div.col.s12
          [input/text {:placeholder "Description" :atom description}]]
         [:div.col.m6.s12
          [:div.input-field
           ;; Styles defined in resources/public/css/styles.css
           [input/datepicker {:atom date}]]]
         [:div.input-field.col.m6.s12
          [input/select
           {:atom        time
            :placeholder "Choose a Time"
            :options     (map (fn [[val label]]
                                {:value (name val)
                                 :label label})
                              rooms/sorted-times)}]]]]
       [:div.modal-footer
        [:button.btn-flat.amber-text.darken-1.waves-effect.waves-purple
         {:on-click (fn []
                      (rf/dispatch [:room/post {:title        @title
                                                :max-capacity @capacity
                                                :room-number  @number
                                                :description  @description
                                                :date         @date
                                                :time         @time}]))}
         "Submit"]]])))


(defn student
  "One student in the attendance list."
  [room user]
  [:li.collection-item
   {:key (:id user)}
   [:div.row.valign-wrapper
    {:style {:margin-bottom "0px"}}
    [:div.col.s6
     [:span.left
      {:style {:color (case (:attendance user)
                        -1 :red
                        1  :green
                        nil)}}
      (:name user)]]
    [:div.col.s6
     [:div.right-align
      [:a.btn-flat.green-text.waves-effect.waves-green
       {:on-click #(rf/dispatch [:room/attendance
                                 (:id room)
                                 (:id user)
                                 1])}
       "Present"]
      [:a.btn-flat.red-text.waves-effect.waves-red
       {:on-click #(rf/dispatch [:room/attendance
                                 (:id room)
                                 (:id user)
                                 -1])}
       "Absent"]]]]])

(defn attendance []
  (let [room            @(rf/subscribe [:rooms/attendance-modal])
        students        (rooms/get-students room)
        students-sorted (->> students
                             (sort-by :name)
                             (sort-by :attendance)
                             (sort-by (fn [student]
                                        (not (zero?
                                              (:attendance student))))))]
    [:div.modal-content
     [:h4.purple-text.text-lighten-3 "Students"]
     [:div.row
      [:div.col.l8.offset-l2.s12
       (if (seq students)
         [:ul.collection
          (map (partial student room) students-sorted)]
         [:h6.amber-text.center "No students have joined yet."])]]]))

(defn attendance-modal
  "The bottom sheet modal that shows a list of students."
  []
  [modal/bottom-sheet {:id "attendance-modal"}
   [attendance]])

(defn buttons
  "Returns the appropriate actions that a user can take on a `room`."
  [room]
  (let [{:keys [id title users description date room-number max-capacity]}
        room
        user          @(rf/subscribe [:login/user])
        user-in-room? ((->> users (map :id) (apply hash-set)) (:id user))]
    [:div.card-action
     (cond ; Actions
       (and (not (:teacher user))
            (not user-in-room?))
       [:a.btn-flat.amber-text.waves-effect.waves-purple
        {:on-click #(rf/dispatch [:room/join id])} "Join"]

       (and (not (:teacher user))
            user-in-room?)
       [:a.btn-flat.amber-text.waves-effect.waves-purple
        {:on-click #(rf/dispatch [:room/leave id])} "Leave"]

       (and (:teacher user)
            user-in-room?)
       [:a.btn-flat.amber-text.waves-effect.waves-purple
        {:on-click #(rf/dispatch [:room/delete id])} "Delete"]

       :else
       [:div])
     (cond ;Information
       (or (:teacher user)
           (:admin user))
       [:a.btn-flat.amber-text.waves-effect.waves-purple
        {:on-click #(rf/dispatch [:rooms/set-attendance-modal room])}
        "Students"]

       :else
       [:div])]))


(defn card
  "Creates a card with information about a `room`."
  [room]
  (when-let [{:keys [id title users description date time room-number max-capacity]} room]
    (let [search (rf/subscribe [:search])]
      [:div.col.s12.m6.l4.grid-item
       {:key   id
        ;; Used for easter eggs. Styles are defined in styles.css
        :class (condp = (str/lower-case @search)
                 ;; Rotate the card by 2 degrees.
                 "askew"            "askew"
                 ;; Does a barrel roll.
                 "do a barrel roll" "barrel-roll"
                 "")}
       [:div.card.hoverable
        [:div.card-content
         [:span.card-title.truncate title]
         [:h6.truncate (or (->> users (filter :teacher) first :name) "")]
         [:span (.toDateString date)]
         [:p (rooms/time-str room)]
         [:p (rooms/room-number-str room)]
         [:p (str (->> users (remove :teacher) count) "/" max-capacity)]]
        [:div.divider]
        [:div.card-content
         {:style {:overflow :hidden}}
         [:p description]]
        [buttons room]]])))

(defn fab []
  (when (and
         (:teacher @(rf/subscribe [:login/user]))
         (not (str/blank? @(rf/subscribe [:login/token]))))
    [:div {:style {:z-index  1
                   :position :fixed
                   :right    24
                   :bottom   24}}
     [:a.btn-floating.btn-large.amber.hoverable.modal-trigger
      {:href "#add-room-modal"}
      [:i.large.material-icons "add"]]]))

(defn grid
  "Returns a grid of rooms."
  []
  (let [token (rf/subscribe [:login/token])
        rooms (rf/subscribe [:rooms/sorted])]
    (when-not (empty? @token)
      [:div.row
       (if-not (seq @rooms)
         ;; Get rooms if rooms are empty.
         (rf/dispatch [:rooms/get])
         ;; Otherwise show the rooms
         [grid/grid (doall (map card (take 36 @rooms)))])])))


(defn filters []
  (let [filters @(rf/subscribe [:rooms/time-filter])
        show?   @(rf/subscribe [:rooms/filter])]
    [:div.row
     (when show?
       (for [[k s] rooms/sorted-times]
         [:div.col.s6.m3
          {:key k}
          [:label
           [:input
            {:type      :checkbox
             :value     (filters k)
             :on-change #(rf/dispatch [:rooms/update-time-filter
                                       k
                                       (-> %
                                           .-target
                                           .-checked)])}]
           [:span s]]]))
     [:div.col.s12.center
      {:style {:padding-top (if show? "2vh" "0px")}}
      [:a
       {:style    {:cursor :pointer}
        :on-click #(rf/dispatch [:rooms/toggle-filter])}
       (if show? "Hide Filters" "Show Filters")]]]))


(defn page
  "Root component for the rooms page.
  Consists of a search bar, a grid of rooms, and a FAB that can be
  used to open a form for adding rooms."
  []
  [:div.container
   (when-not (str/blank? @(rf/subscribe [:login/token]))
     [:div
      [search/search-bar]
      [fab]
      [add]
      [filters]
      [grid]
      [attendance-modal]])])

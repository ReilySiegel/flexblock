(ns flexblock.components.room
  "Render functions for elements related to showing rooms."
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [clojure.string :as str]
   [flexblock.rooms :as room]
   [flexblock.rooms :as rm]
   [flexblock.components.input :as input]
   [flexblock.components.attendance :as attendance]
   [flexblock.components.grid :as grid]
   [flexblock.components.modal :as modal]
   [flexblock.components.search :as search])
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
                       :atom        number
                       :type        :number}]]
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
            :options     [{:value "after" :label "After School"}
                          {:value "before" :label "Before School"}
                          {:value "flex" :label "Flex Block"}]}]]]]
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

(defn- buttons
  "Returns the appropriate actions that a user can take on a `room`."
  [room]
  (let [{:keys [id title users description date room-number max-capacity]}
        room
        user          @(rf/subscribe [:user])
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
       (and (:teacher user))
       [:a.btn-flat.amber-text.waves-effect.waves-purple.modal-trigger
        {:href (str "#" (attendance/get-id room))} "Students"]

       :else
       [:div])]))

(defn card
  "Creates a card with information about a `room`."
  [room]
  (when-let [{:keys [id title users description date time room-number max-capacity]} room]
    [:div.col.s12.m6.l4.grid-item
     {:key id}
     [:div.card.hoverable
      [:div.card-content
       [:span.card-title.truncate title]
       [:h6.truncate (or (->> users (filter :teacher) first :name) "")]
       [:span (.toDateString date)]
       [:p ((keyword time) {:before "Before School"
                            :after  "After School"
                            :flex   "FlexBlock"} "")]
       [:p (str "Room: " room-number)]
       [:p (str (->> users (remove :teacher) count) "/" max-capacity)]]
      [:div.divider]
      [:div.card-content
       {:style {:overflow :hidden}}
       [:p description]]
      [buttons room]]]))

(defn fab []
  (when (and
         (:teacher @(rf/subscribe [:user]))
         (not (str/blank? @(rf/subscribe [:token]))))
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
  (rf/dispatch [:room/get])
  (fn []
    (let [rooms  (rf/subscribe [:rooms])
          search (rm/make-search @rooms @(rf/subscribe [:search]))]
      [:div.container
       [search/search-bar]
       [grid/grid
        (doall
         (->> @rooms
              (sort-by :date)
              (sort-by #(not= (:name @(rf/subscribe [:user]))
                              (:name (rm/get-teacher %))))
              ;; Search gives higher numbers for better matches, so we
              ;; need to sort in descending order.
              (sort-by search #(compare %2 %1))
              (map card)))]
       (doall (map attendance/modal @rooms))])))

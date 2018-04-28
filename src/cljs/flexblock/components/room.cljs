
(ns flexblock.components.room
  "Render functions for elements related to showing rooms."
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [clojure.string :as str]
   [flexblock.rooms :as room]
   [flexblock.utils :as u]
   [flexblock.rooms :as rm]
   [flexblock.components.input :as input :refer [input-rf-dispatch]]
   [flexblock.components.attendance :as attendance]
   [flexblock.components.modal :as modal]
   [flexblock.components.search :as search])
  (:import goog.date.Date))

(defn form
  "The form for creating a new room."
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
        [input-rf-dispatch
         {:placeholder "Title"
          :class-name  "charcount room-form"
          :data-length 50} "Title" :add-room/set-title :room/title true]]
       [:div.col.l6.m12
        [input-rf-dispatch
         {:placeholder "Room Number"
          :class-name  "room-form"
          :type        :number} "Room Number" :add-room/set-room-number :room/number]]
       [:div.col.l6.m12
        [input-rf-dispatch
         {:placeholder "Max Capacity"
          :class-name  "room-form"
          :type        :number}
         "Max Capacity" :add-room/set-max-capacity :room/max-capacity]]
       [:div.col.s12
        [input-rf-dispatch
         {:placeholder "Description" 
          :class-name  "charcount room-form"
          :data-length 250} "Description" :add-room/set-description :room/description true]]

       [:div.col.m6.s12
        [:div.input-field
         ;; Styles defined in resources/public/css/styles.css, options
         ;; defined above.
         [input/datepicker {:dispatch-key :add-room/set-date}]]]
       
       [:div.input-field.col.l6.m12
        [:select
         {:on-change     #(rf/dispatch [:add-room/set-time (-> % .-target .-value)])
          :default-value ""
          :class-name    "room-form"}
         [:option {:value    ""
                   :disabled true} "Choose a time"]
         [:option {:value :after} "After School"]
         [:option {:value :before} "Before School"]
         [:option {:value :flex} "Flex Block"]]]])}))

(defn add
  "The modal that contains `flexblock.components.room/form`."
  [] 
  [modal/fixed-footer "add-room-modal"
   [:div.modal-content 
    [:h4.center.purple-text.text-lighten-3 "Add Session"]
    [form]] 
   [:div.modal-footer 
    [:a.btn-flat.amber-text.darken-1.waves-effect.waves-purple
     {:on-click u/post-room}
     "Submit"]]])

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
        {:on-click #(u/join-room id)} "Join"]
       
       (and (not (:teacher user))
            user-in-room?)
       [:a.btn-flat.amber-text.waves-effect.waves-purple
        {:on-click #(u/leave-room id)} "Leave"]

       (and (:teacher user)
            user-in-room?)
       [:a.btn-flat.amber-text.waves-effect.waves-purple
        {:on-click #(u/delete-room id)} "Delete"]

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
    [:div {:style {:position :fixed
                   :right    24
                   :bottom   24}}
     [:a.btn-floating.btn-large.amber.hoverable.modal-trigger
      {:href "#add-room-modal"}
      [:i.large.material-icons "add"]]]))

(defn grid
  "Returns a grid of rooms."
  [] 
  (letfn [(layout [& args] 
            (new js/Masonry
                 (.querySelector js/document ".grid-room")
                 (clj->js {:itemSelector    ".grid-item"
                           :horizontalOrder true})))]      
    (r/create-class
     {:component-did-mount  layout
      :component-did-update layout
      :reagent-render
      (fn []
        (let [rooms (rf/subscribe [:rooms])]
          (if (seq @rooms) 
            [:div.container
             [search/search-bar]
             [:div.row.grid-room 
              (doall
               (->> @rooms
                    (sort-by :date) 
                    (sort-by #(not= (:name @(rf/subscribe [:user]))
                                    (:name (rm/get-teacher %))))
                    (sort-by #(rm/search @(rf/subscribe [:search]) %))
                    (map card)))
              (doall (map attendance/modal @rooms))]]
            [:div.grid-room])))})))

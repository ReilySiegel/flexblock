(ns flexblock.rooms.events
  (:require [re-frame.core :as rf]
            [ajax.core :as ajax]
            [flexblock.db :as db]
            [goog.string :as gstring]
            [goog.string.format]))

(rf/reg-event-fx
 :rooms/set
 (fn [{:keys [db]} [_ rooms]]
   {:db       (assoc db :rooms rooms)
    :dispatch [:rooms/get-attendance]}))

(rf/reg-event-db
 :rooms/toggle-filter
 (fn [db _]
   (update db :rooms/filter not)))

(rf/reg-event-db
 :rooms/update-time-filter
 (fn [db [_ key filter?]]
   (if filter?
     (update db :rooms/time-filter conj key)
     (update db :rooms/time-filter disj key))))


(rf/reg-event-fx
 :rooms/get
 (fn [{:keys [db]} _]
   {:http-xhrio {:method          :get
                 :uri             "/rooms"
                 :format          (ajax/transit-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:rooms/set]
                 :on-failure      [:http/failure]}}))

(rf/reg-event-fx
 :room/post-success
 (fn [_ [_ response]]
   {:notification "Room added."
    :dispatch     [:rooms/get]
    :close-modal  "#add-room-modal"}))

(rf/reg-event-fx
 :room/post
 (fn [{:keys [db]} [_ room]]
   {:http-xhrio {:method          :post
                 :uri             "/rooms"
                 :params          (-> room
                                      (update :max-capacity js/parseInt))
                 :format          (ajax/transit-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:room/post-success]
                 :on-failure      [:http/failure]}}))

(rf/reg-event-fx
 :room/join-success
 (fn [_ [_ response]]
   {:notification "Room joined."
    :dispatch     [:rooms/get]}))

(rf/reg-event-fx
 :room/join
 (fn [{:keys [db]} [_ room-id]]
   {:http-xhrio {:method          :post
                 :uri             (gstring/format "/rooms/%s/join"
                                                  room-id)
                 :format          (ajax/transit-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:room/join-success]
                 :on-failure      [:http/failure]}}))

(rf/reg-event-fx
 :room/leave-success
 (fn [_ [_ response]]
   {:notification "Room left."
    :dispatch     [:rooms/get]}))

(rf/reg-event-fx
 :room/leave
 (fn [{:keys [db]} [_ room-id]]
   {:http-xhrio {:method          :post
                 :uri             (gstring/format "/rooms/%s/leave"
                                                  room-id)
                 :format          (ajax/transit-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:room/leave-success]
                 :on-failure      [:http/failure]}}))

(rf/reg-event-fx
 :room/delete-success
 (fn [_ [_ response]]
   {:notification "Room deleted."
    :dispatch     [:rooms/get]}))

(rf/reg-event-fx
 :room/delete
 (fn [{:keys [db]} [_ room-id]]
   {:http-xhrio {:method          :delete
                 :uri             (str "/rooms/" room-id)
                 :format          (ajax/transit-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:room/delete-success]
                 :on-failure      [:http/failure]}}))

(rf/reg-event-fx
 :room/set-attendance-success
 (fn [_ [_ response]]
   {:dispatch [:rooms/get-attendance]}))

(rf/reg-event-fx
 :room/set-attendance
 (fn [{:keys [db]} [_ room-id user-id attendance]]
   {:http-xhrio {:method          :patch
                 :uri             (gstring/format
                                   "/rooms/%s/attendance"
                                   room-id)
                 :params          {:id         user-id
                                   :attendance attendance}
                 :format          (ajax/transit-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:room/set-attendance-success]
                 :on-failure      [:http/failure]}}))

(rf/reg-event-fx
 :rooms/get-attendance-success
 (fn [{:keys [db]} [_ response]]
   {:db (assoc db :attendance response)}))

(rf/reg-event-fx
 :rooms/get-attendance
 (fn [{:keys [db]} [_ room-id user-id attendance]]
   {:http-xhrio {:method          :get
                 :uri             "/rooms/attendance"
                 :format          (ajax/transit-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:rooms/get-attendance-success]
                 :on-failure      [:http/failure]}}))

(rf/reg-event-fx
 :rooms/set-attendance-modal
 (fn [{:keys [db]} [_ room]]
   {:db         (assoc db :attendance-modal room)
    :open-modal "#attendance-modal"}))

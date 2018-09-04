(ns flexblock.rooms.events
  (:require [re-frame.core :as rf]
            [ajax.core :as ajax]
            [flexblock.db :as db]))

(rf/reg-event-fx
 :rooms/set
 (fn [{:keys [db]} [_ rooms]]
   {:db (assoc db :rooms rooms)}))

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
                 :uri             "/room"
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
                 :uri             "/room"
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
                 :uri             "/room/join"
                 :params          {:room-id room-id}
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
                 :uri             "/room/leave"
                 :params          {:room-id room-id}
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
                 :uri             "/room"
                 :params          {:room-id room-id}
                 :format          (ajax/transit-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:room/delete-success]
                 :on-failure      [:http/failure]}}))

(rf/reg-event-fx
 :room/attendance-success
 (fn [_ [_ response]]
   {:dispatch [:rooms/get]}))

(rf/reg-event-fx
 :room/attendance
 (fn [{:keys [db]} [_ room-id user-id attendance]]
   {:http-xhrio {:method          :post
                 :uri             "/room/attendance"
                 :params          {:room-id    room-id
                                   :user-id    user-id
                                   :attendance attendance}
                 :format          (ajax/transit-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:room/attendance-success]
                 :on-failure      [:http/failure]}}))

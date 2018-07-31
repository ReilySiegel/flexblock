(ns flexblock.events.room
  (:require [re-frame.core :as rf]
            [ajax.core :as ajax]
            [flexblock.db :as db]))

(rf/reg-event-fx
 :room/set
 (fn [{:keys [db]} [_ rooms]]
   {:db (assoc db :rooms rooms)}))

(rf/reg-event-fx
 :room/get
 (fn [{:keys [db]} _]
   {:http-xhrio {:method          :get
                 :uri             "/room"
                 :headers         {"Authorization" (str "Token "
                                                        (:token db))}
                 :format          (ajax/transit-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:room/set]
                 :on-failure      [:http/failure]}}))

(rf/reg-event-fx
 :room/post-success
 (fn [_ [_ response]]
   {:notification "Room added."
    :dispatch     [:room/get]
    :close-modal  "#add-room-modal"}))

(rf/reg-event-fx
 :room/post
 (fn [{:keys [db]} [_ room]]
   {:http-xhrio {:method          :post
                 :uri             "/room"
                 :headers         {"Authorization" (str "Token "
                                                        (:token db))}
                 :params          (-> room
                                      (update :room-number js/parseInt)
                                      (update :max-capacity js/parseInt))
                 :format          (ajax/transit-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:room/post-success]
                 :on-failure      [:http/failure]}}))

(rf/reg-event-fx
 :room/join-success
 (fn [_ [_ response]]
   {:notification "Room joined."
    :dispatch     [:room/get]}))

(rf/reg-event-fx
 :room/join
 (fn [{:keys [db]} [_ room-id]]
   {:http-xhrio {:method          :post
                 :uri             "/room/join"
                 :headers         {"Authorization" (str "Token "
                                                        (:token db))}
                 :params          {:room-id room-id}
                 :format          (ajax/transit-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:room/join-success]
                 :on-failure      [:http/failure]}}))

(rf/reg-event-fx
 :room/leave-success
 (fn [_ [_ response]]
   {:notification "Room left."
    :dispatch     [:room/get]}))

(rf/reg-event-fx
 :room/leave
 (fn [{:keys [db]} [_ room-id]]
   {:http-xhrio {:method          :post
                 :uri             "/room/leave"
                 :headers         {"Authorization" (str "Token "
                                                        (:token db))}
                 :params          {:room-id room-id}
                 :format          (ajax/transit-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:room/leave-success]
                 :on-failure      [:http/failure]}}))

(rf/reg-event-fx
 :room/delete-success
 (fn [_ [_ response]]
   {:notification "Room deleted."
    :dispatch     [:room/get]}))

(rf/reg-event-fx
 :room/delete
 (fn [{:keys [db]} [_ room-id]]
   {:http-xhrio {:method          :delete
                 :uri             "/room"
                 :headers         {"Authorization" (str "Token "
                                                        (:token db))}
                 :params          {:room-id room-id}
                 :format          (ajax/transit-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:room/delete-success]
                 :on-failure      [:http/failure]}}))

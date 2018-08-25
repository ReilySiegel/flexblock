(ns flexblock.reminder.events
  (:require [ajax.core :as ajax]
            [re-frame.core :as rf]))

(rf/reg-event-fx
 :reminder/post-date-success
 (fn [_ [_ response]]
   {:notification "Mail sent."
    :close-modal  "#emailermodal"}))

(rf/reg-event-fx
 :reminder/post-date
 (fn [{:keys [db]}]
   {:http-xhrio {:method          :post
                 :uri             "/user/flexblock"
                 :headers         {"Authorization" (str "Token " (:token db))}
                 :params          {:date (:date db)}
                 :format          (ajax/json-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:reminder/post-date-success]
                 :on-failure      [:http/failure]}}))

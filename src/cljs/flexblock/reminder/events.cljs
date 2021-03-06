(ns flexblock.reminder.events
  (:require [ajax.core :as ajax]
            [flexblock.users.subs :as users.subs]
            [re-frame.core :as rf]))

(rf/reg-event-fx
 :reminder/post-date-success
 (fn [_ [_ response]]
   {:notification "Mail sent."
    :dispatch-n   [[:reminder/set-open false]]}))

(rf/reg-event-fx
 :reminder/post-date
 (fn [{:keys [db]}]
   {:http-xhrio {:method          :post
                 :uri             "/users/flexblock"
                 :params          {:date (:date db)
                                   :ids  (map :id
                                              (users.subs/filter-users
                                               (:users db)
                                               (:login/user db)
                                               (:date db)
                                               (:users/role-filter db)))}
                 :format          (ajax/json-request-format)
                 :response-format (ajax/detect-response-format)
                 :on-success      [:reminder/post-date-success]
                 :on-failure      [:http/failure]}}))

(rf/reg-event-db
 :reminder/set-open
 (fn [db [_ open?]]
   (assoc db :reminder/open open?)))

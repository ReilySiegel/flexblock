(ns flexblock.navbar.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
 :set-active-page
 (fn [db [_ page]]
   (assoc db :page page)))

(rf/reg-event-db
 :navbar/set-options-open
 (fn [db [_ open?]]
   (assoc db :navbar/options-open open?)))

(rf/reg-event-db
 :navbar/set-page-zoom
 (fn [db [_ zoom?]]
   (assoc db :navbar/page-zoom zoom?)))

(rf/reg-event-fx
 :navbar/swap-page
 (fn [{:keys [db]} _]
   (merge
    (if (and (some #(% (:login/user db)) [:teacher :admin])
             (not (empty? (:login/token db))))
      (cond
        (or (not (some #(% (:login/user db)) [:teacher :admin]))
            (empty? (:login/token db)))
        {}

        ( = :rooms (:page db))
        {:dispatch-n [[:set-active-page :users]
                      [:navbar/set-page-zoom false]]}

        (= :users (:page db))
        {:dispatch-n [[:set-active-page :rooms]
                      [:navbar/set-page-zoom false]]}

        :else {}))
    {:dispatch-later [{:ms       500
                       :dispatch [:navbar/set-page-zoom true]}]})))

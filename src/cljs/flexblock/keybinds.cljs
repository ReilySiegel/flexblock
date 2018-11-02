(ns flexblock.keybinds
  (:require [flexblock.users :as users]
            [re-frame.core :as rf]
            [re-pressed.core :as rp]))

(rf/reg-event-fx
 :key/set-main-modal
 (fn [{:keys [db]} [_ open?]]
   (cond
     (and (= :rooms (:page db))
          (:teacher (:login/user db)))
     {:dispatch [:rooms/set-modal-open open?]}

     (and (= :users (:page db))
          (some #{:teacher :admin}  (users/user-roles (:login/user db))))
     {:dispatch [:users/set-modal-open open?]}
     :else {})))

(rf/reg-event-fx
 :key/try-login
 (fn [{:keys [db]} _]
   (if (empty? (:login/token db))
     {:dispatch [:login/set-open true]}
     {})))

(rf/reg-event-fx
 :key/refresh
 (fn [{:keys [db]} _]
   {:dispatch-n [[:rooms/get]
                 [:users/get]]}))

(defn init-keybindings! []
  (rf/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (rf/dispatch
   [::rp/set-keydown-rules
    {:event-keys [[[:key/set-main-modal true]
                   ;; Press a (add).
                   [{:which 65}]
                   ;; Press n (new).
                   [{:which 78}]]
                  [[:key/try-login]
                   ;; Press l (login).
                   [{:which 76}]]
                  [[:navbar/swap-page]
                   ;; Press p (page).
                   [{:which 80}]]
                  [[:navbar/set-options-open true]
                   ;; Press o (options).
                   [{:which 79}]]
                  [[:key/refresh]
                   ;; Press r (refresh).
                   [{:which 82}]]
                  [[:search/focus]
                   ;; Press s (search).
                   [{:which 83}]]]}]))

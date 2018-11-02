(ns flexblock.keybinds
  (:require [re-frame.core :as rf]
            [re-pressed.core :as rp]))

(rf/reg-event-fx
 :key/set-main-modal
 (fn [{:keys [db]} [_ open?]]
   (case (:page db)
     :rooms {:dispatch [:rooms/set-modal-open open?]}
     :users {:dispatch [:users/set-modal-open open?]}
     {})))

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

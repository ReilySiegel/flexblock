(ns flexblock.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType] 
            [ajax.core :refer [GET POST]]
            [flexblock.ajax :refer [load-interceptors!]]
            [flexblock.events]
            [flexblock.effects]
            [flexblock.utils :as u] 
            [flexblock.components.login :as login]
            [flexblock.pages.rooms :as rooms]
            [flexblock.pages.students :as students])
  (:import goog.History))

(defn nav-link [uri title page]
  [:li.nav-item
   {:class (when (= page @(rf/subscribe [:page])) "active")}
   [:a.nav-link {:href uri} title]])

(defn page-button []
  (let [page @(rf/subscribe [:page])]
    (condp = page
      :rooms    [:li
                 {:on-click #(rf/dispatch [:set-active-page :students])}
                 [:a "Students"]]
      :students [:li
                 {:on-click #(rf/dispatch [:set-active-page :rooms])}
                 [:a "Sessions"]])))

(defn navbar []
  (let [loading (rf/subscribe [:loading])]
    [:nav
     [:div.nav-wrapper.purple
      [:a.brand-logo.center 
       {:on-click #(do (u/get-rooms)
                       (u/get-users))
        :style    {:cursor :pointer}}
       "FlexBlock"]
      [:ul.left
       (if (:teacher @(rf/subscribe [:user]))
         [page-button])]
      [:ul.right
       (if (empty? @(rf/subscribe [:token]))
         [:li [:a.modal-trigger {:href "#login-modal"} "Login"]]
         [:li [:a {:on-click (fn [_]
                               (rf/dispatch [:set-rooms []])
                               (rf/dispatch [:set-user {}])
                               (rf/dispatch [:set-token ""])
                               (rf/dispatch [:set-active-page :rooms]))}
               "Logout"]])]
      [:ul.left.hide-on-med-and-down]
      (when (pos? @loading)
        [:div.progress.purple.lighten-3
         [:div.indeterminate.amber]])]]))

(def pages
  {:rooms    #'rooms/page
   :students #'students/page})

(defn page []
  [:div
   [navbar]
   [login/modal]
   [(pages @(rf/subscribe [:page]))]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (rf/dispatch [:set-active-page :rooms]))

(secretary/defroute "/students" []
  (rf/dispatch [:set-active-page :students]))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     HistoryEventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db]) 
  (load-interceptors!) 
  (hook-browser-navigation!)
  (mount-components))

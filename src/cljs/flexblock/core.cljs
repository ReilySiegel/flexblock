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
            [flexblock.components.navbar :as navbar]
            [flexblock.pages.rooms :as rooms]
            [flexblock.pages.students :as students])
  (:import goog.History))

(def pages
  {:rooms    #'rooms/page
   :students #'students/page})

(defn page []
  [:div
   [navbar/navbar]
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

(ns flexblock.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [ajax.core :refer [GET POST]]
            [flexblock.ajax :refer [load-interceptors!]]
            [flexblock.events]
            [flexblock.effects]
            [flexblock.components.beta :as beta]
            [flexblock.components.login :as login]
            [flexblock.components.navbar :as navbar]
            ;; Konami Code Easter Egg
            [flexblock.components.konami :as konami]
            [flexblock.pages.rooms :as rooms]
            [flexblock.pages.students :as students]))

(def pages
  {:rooms    #'rooms/page
   :students #'students/page})

(defn page []
  [:div
   [navbar/navbar]
   [login/modal]
   ;; Load the beta disclaimer here, as this is the first page visible.
   [beta/disclaimer]
   ;; Konami Code Easter Egg
   [konami/egg]
   [(pages @(rf/subscribe [:page]))]])

(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (load-interceptors!)
  (js/setTimeout mount-components 2000))

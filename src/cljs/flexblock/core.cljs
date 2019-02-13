(ns flexblock.core
  "The core namespace for Flexblock CLJS.
  This is the entry-point for the front-end code of Flexblock."
  (:require
   ;; Reagent and re-frame are used to generate react components based
   ;; on CLJS data structures, and manage the state of such components.
   [reagent.core :as r]
   [re-frame.core :as rf]
   ;; Load interceptors, which makes automatically injects CSRF and
   ;; AUTH tokens into requests.
   [flexblock.ajax :refer [load-interceptors!]]
   ;; Load global subscription handlers.
   [flexblock.subs]
   ;; Load global event handlers.
   ;; Event handlers allow components to update the application
   ;; state. For more details, see re-frame documentation.
   [flexblock.events]
   ;; Load effects.
   ;; Effects are used to manage side-effecting code (such as
   ;; displaying a notification) inside re-frame event handlers. See
   ;; re-frame documentation for more details.
   [flexblock.effects]
   [flexblock.keybinds :as keybinds]
   ;; Load components that need to be displayed on every page.
   [flexblock.about.views :as about]
   [flexblock.login.views :as login]
   [flexblock.navbar.views :as navbar]
   [flexblock.snackbar.views :as snackbar]
   [flexblock.users.views :as users.views]
   [flexblock.components.material :as material]
   ;; Load the rooms and student pages.
   [flexblock.rooms.views :as rooms]
   [flexblock.users.views :as users]))

(def pages
  {:rooms #'rooms/page
   :users #'users.views/page})

(defn page []
  [material/MuiThemeProvider
   {:theme (material/createMuiTheme
            {:typography {:useNextVariants true}
             :palette
             {:primary   (material/color :deepPurple)
              :secondary (material/color :deepOrange)
              :type      @(rf/subscribe [:theme])}})}
   [material/CssBaseline
    [navbar/navbar]
    [login/modal]
    [(pages @(rf/subscribe [:page]))]
    [users.views/password-modal]
    [about/modal]
    [snackbar/snackbar]]])

(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (js/setInterval (fn []
                    (rf/dispatch-sync [:rooms/get])
                    (rf/dispatch-sync [:users/get]))
                  (* 60 1000))
  (load-interceptors!)
  (keybinds/init-keybindings!)
  (mount-components))

(defn reload! []
  (mount-components))

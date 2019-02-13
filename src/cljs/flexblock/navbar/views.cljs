(ns flexblock.navbar.views
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [flexblock.components.material :as material]
            [flexblock.users.views :as users]))

(defn nav-link [uri title page]
  [:li.nav-item
   {:class (when (= page @(rf/subscribe [:page])) "active")}
   [:a.nav-link {:href uri} title]])

(defn page-button []
  (let [page            (rf/subscribe [:page])
        zoom?           (rf/subscribe [:navbar/page-zoom])
        [active-page
         inactive-page] (if (= :rooms @page)
                          ["Users" "Sessions"]
                          ["Sessions" "Users"])]
    [material/Slide
     {:in        @zoom?
      :direction :right}
     [material/Button
      {:color   :inherit
       :onClick #(rf/dispatch [:navbar/swap-page
                               (if (= :rooms @page)
                                 :users
                                 :rooms)])}
      (if @zoom?
        active-page
        inactive-page)]]))

(defn user-options [user]
  (fn []
    [:div
     [material/Slide
      {:in        true
       :direction :left}
      [material/IconButton
       {:id      :user-options
        :color   :inherit
        :onClick #(rf/dispatch [:navbar/set-options-open true])}
       [:i.material-icons :account_circle]]]
     [material/Menu
      {:anchorEl (.getElementById js/document "user-options")
       :open     @(rf/subscribe [:navbar/options-open])
       :onClose  #(rf/dispatch [:navbar/set-options-open false])}
      [material/MenuItem
       {:on-click (fn []
                    (rf/dispatch [:navbar/set-options-open false])
                    (rf/dispatch [:theme/toggle]))}
       (case @(rf/subscribe [:theme])
         :dark  "Light Theme"
         :light "Dark Theme")]
      [material/MenuItem
       {:onClick (fn []
                   (rf/dispatch [:users/set-password-modal
                                 @(rf/subscribe [:login/user])])
                   (rf/dispatch [:navbar/set-options-open false]))}
       "Reset Password"]
      [material/MenuItem
       {:onClick (fn []
                   (rf/dispatch [:navbar/set-options-open false])
                   (rf/dispatch [:logout]))}
       "Log Out"]
      [material/MenuItem
       {:onClick (fn []
                   (rf/dispatch [:navbar/set-options-open false])
                   (rf/dispatch [:about/open? true]))}
       "About"]]]))

(defn navbar []
  (let [user  (rf/subscribe [:login/user])
        token (rf/subscribe [:login/token])]
    [material/Slide
     {:in true}
     [material/AppBar {:position :sticky}
      [material/Toolbar
       [material/Grid
        {:container  true
         :justify    :space-between
         :alignItems :center}
        (when (and (some #(% @user) [:teacher :admin])
                   (not (empty? @token)))
          [material/Grid
           {:item true
            :xs   4}
           [page-button]])
        [material/Grid
         {:item true
          :xs   4}
         [material/Typography
          {:variant :h5
           :color   :inherit
           :align   :center}
          "Flexblock"]]
        [material/Grid
         {:item true
          :xs   4}
         [:div
          {:style {:float :right}}
          (if (empty? @token)
            [material/Button
             {:color   :inherit
              :onClick #(rf/dispatch [:login/set-open true])}
             "Log In"]
            [user-options user])]]]]]]))

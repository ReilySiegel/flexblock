(ns flexblock.navbar.views
  (:require [re-frame.core :as rf]
            [flexblock.components.input :as input]
            [flexblock.users.views :as users]))

(defn nav-link [uri title page]
  [:li.nav-item
   {:class (when (= page @(rf/subscribe [:page])) "active")}
   [:a.nav-link {:href uri} title]])

(defn page-button []
  (let [page @(rf/subscribe [:page])]
    (condp = page
      :rooms    [:li
                 {:on-click #(rf/dispatch [:set-active-page :students])}
                 [:a (if (:admin @(rf/subscribe [:login/user]))
                       "Users"
                       "Students")]]
      :students [:li
                 {:on-click #(rf/dispatch [:set-active-page :rooms])}
                 [:a "Sessions"]])))

(defn navbar []
  (let [user  (rf/subscribe [:login/user])
        token (rf/subscribe [:login/token])]
    [:div
     [users/password-modal @user {:id "reset-password-modal"}]
     [:nav
      [:div.nav-wrapper.purple
       [:a.brand-logo.center
        {:on-click (fn []
                     (rf/dispatch [:rooms/get])
                     (rf/dispatch [:users/get]))
         :style    {:cursor :pointer}}
        "FlexBlock"]
       [:ul.left
        (if (and (some #(% @user) [:teacher :admin])
                 (not (empty? @token)))
          [page-button])]
       [:ul.right
        (if (empty? @token)
          [:li [:a.modal-trigger
                {:on-click #(input/focus "login-username-input")
                 :href     "#login-modal"} "Login"]]
          [:div
           [:li.hide-on-small-only
            [:a.modal-trigger {:href "#reset-password-modal"}
             "Reset Password"]]
           [:li [:a {:on-click #(rf/dispatch [:logout])}
                 "Logout"]]])]]]]))

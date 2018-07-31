(ns flexblock.components.navbar
  (:require [re-frame.core :as rf]
            [flexblock.components.input :as input]
            [flexblock.components.password :as password]))

(defn nav-link [uri title page]
  [:li.nav-item
   {:class (when (= page @(rf/subscribe [:page])) "active")}
   [:a.nav-link {:href uri} title]])

(defn page-button []
  (let [page @(rf/subscribe [:page])]
    (condp = page
      :rooms    [:li
                 {:on-click #(rf/dispatch [:set-active-page :students])}
                 [:a (if (:admin @(rf/subscribe [:user]))
                       "Users"
                       "Students")]]
      :students [:li
                 {:on-click #(rf/dispatch [:set-active-page :rooms])}
                 [:a "Sessions"]])))

(defn navbar []
  (let [user (rf/subscribe [:user])]
    [:div
     [password/modal @user {:id "reset-password-modal"}]
     [:nav
      [:div.nav-wrapper.purple
       [:a.brand-logo.center
        {:on-click (fn []
                     (rf/dispatch [:room/get])
                     (rf/dispatch [:user/get]))
         :style    {:cursor :pointer}}
        "FlexBlock"]
       [:ul.left
        (if (some #(% @user) [:teacher :admin])
          [page-button])]
       [:ul.right
        (if (empty? @(rf/subscribe [:token]))
          [:li [:a.modal-trigger
                {:on-click #(input/focus "login-username-input")
                 :href     "#login-modal"} "Login"]]
          [:div
           [:li.hide-on-small-only
            [:a.modal-trigger {:href "#reset-password-modal"}
             "Reset Password"]]
           [:li [:a {:on-click #(rf/dispatch [:logout])}
                 "Logout"]]])]]]]))

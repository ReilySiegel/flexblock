(ns flexblock.components.navbar
  (:require [re-frame.core :as rf]
            [flexblock.utils :as u]
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
                 [:a "Students"]]
      :students [:li
                 {:on-click #(rf/dispatch [:set-active-page :rooms])}
                 [:a "Sessions"]])))

(defn navbar []
  (let [loading (rf/subscribe [:loading])
        user    (rf/subscribe [:user])]
    [:divs
     [password/modal @user {:id "reset-password-modal"}]
     [:nav
      [:div.nav-wrapper.purple
       [:a.brand-logo.center
        {:on-click #(do (u/get-rooms)
                        (u/get-users))
         :style    {:cursor :pointer}}
        "FlexBlock"]
       [:ul.left
        (if (some #(% @user) [:teacher :admin])
          [page-button])]
       [:ul.right
        (if (empty? @(rf/subscribe [:token]))
          [:li [:a.modal-trigger {:href "#login-modal"} "Login"]]
          [:div
           [:li [:a.modal-trigger {:href "#reset-password-modal"}
                 "Reset Password"]]
           [:li [:a {:on-click (fn [_]
                                 (rf/dispatch [:set-rooms []])
                                 (rf/dispatch [:set-user {}])
                                 (rf/dispatch [:set-token ""])
                                 (rf/dispatch [:set-active-page :rooms]))}
                 "Logout"]]])]
       [:ul.left.hide-on-med-and-down]
       (when (pos? @loading)
         [:div.progress.purple.lighten-3
          [:div.indeterminate.amber]])]]]))

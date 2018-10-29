(ns flexblock.search.views
  (:require
   [re-com.core :as rc]
   [re-frame.core :as rf]
   [flexblock.components.material :as material]
   [flexblock.reminder.views :as reminder]
   [clojure.string :as str]))

(defn search-bar []
  [material/Grid
   {:container true
    :justify   :center
    :style     {:padding-top "3vh"}}
   [material/Grid
    {:item true
     :lg   6
     :md   8
     :xs   10}
    [material/TextField
     {:fullWidth true
      :label     "Search"
      :id        :search
      :onChange  #(rf/dispatch-sync [:set-search-debounce
                                     (-> %
                                         .-target
                                         .-value)])
      :value     @(rf/subscribe [:search])}]]])

(defn date-picker []
  [material/TextField
   {:label           "Date"
    :fullWidth       true
    :type            :date
    :value           @(rf/subscribe [:date])
    :onChange        #(rf/dispatch [:set-date (-> %
                                                  .-target
                                                  .-value)])
    :InputLabelProps {:shrink true}}])

(defn date-bar []
  [material/Grid
   {:container true
    :justify   :center}
   (if (:admin @(rf/subscribe [:login/user]))
     [material/Grid
      {:container  true
       :justify    :center
       :alignItems :center}
      [material/Grid
       {:item true
        :lg   4
        :sm   6
        :xs   10}
       [date-picker]]
      [material/Grid
       {:item true
        :lg   2
        :sm   4
        :xs   10}
       [reminder/button]]]
     [:div.col.l4.m8.s12.offset-l4.offset-m2
      [date-picker]])])

(ns flexblock.views.home
  "Contains the Hiccup structures for the home page."
  (:require
   [flexblock.views.loading :as loading]
   [hiccup.element :refer [javascript-tag]]
   [hiccup.page :refer [include-js include-css]]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))

(declare ^:dynamic *app-context*)

(def css
  "A list of sylesheets to include in the home page.
  Stylesheets are loaded in order, in the HEAD."
  ["https://fonts.googleapis.com/icon?family=Material+Icons"
   "https://cdnjs.cloudflare.com/ajax/libs/materialize/1.0.0-alpha.3/css/materialize.min.css"
   "css/screen.css"])

(def scripts
  "A list of scripts to include in the home page.
  Scripts are loaded in order, at the end of the BODY."
  ["js/materialize.min.js"
   "js/masonry.pkgd.min.js"
   "js/app.js"])

(defn js-string
  "Defines variable `v` as equal to string `s` in JavaScript included
  in the home page. "
  [v s]
  (javascript-tag
   (format "var %s = \"%s\""
           v s)))

(defn home
  "The Home template."
  []
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name    "viewport"
            :content "width=device-width, initial-scale=1"}]
    (apply include-css css)
    [:title "Flexblock"]]
   [:body
    [:div#app
     [:div.loader-container
      [:div.loader "Loading..."]]
     [:div.center
      [:p (rand-nth loading/messages)]
      [:noscript "Please enable JavaScript."]]]
    (js-string "csrfToken" (or *anti-forgery-token* ""))
    (js-string "context" (or *app-context* ""))
    (apply include-js scripts)]])

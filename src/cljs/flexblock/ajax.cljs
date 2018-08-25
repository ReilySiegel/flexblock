(ns flexblock.ajax
  (:require [ajax.core :as ajax]
            [re-frame.core :as rf]))

(defn local-uri? [{:keys [uri]}]
  (not (re-find #"^\w+?://" uri)))

(defn default-headers [request]
  (let [token @(rf/subscribe [:login/token])]
    (if (local-uri? request)
      (-> request
          (update :uri #(str js/context %))
          (update :headers (partial merge
                                    {"x-csrf-token"  js/csrfToken
                                     "Authorization" (str "Token " token)})))
      request)))

(defn load-interceptors! []
  (swap! ajax/default-interceptors
         conj
         (ajax/to-interceptor {:name    "default headers"
                               :request default-headers})))

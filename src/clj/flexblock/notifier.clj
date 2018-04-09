(ns flexblock.notifier
  (:require [clojure.core.async :as a]
            [mount.core :as mount]
            [flexblock.event :as event]
            [flexblock.mailer :as mailer]
            [flexblock.config :refer [env]]))

(defn do-notify [batch]
  (doall (->> batch
              distinct
              (map event/create-mail)
              (pmap mailer/send))))

(defn notify [in]
  (a/go-loop []
    (when-let [batch (a/<! in)]
      (println "Processing batch of " (count (distinct batch)))
      (do-notify batch)
      (recur))))

(defn batch [in out max-time max-count]
  (let [lim-1 (dec max-count)]
    (a/go-loop [buf [] t (a/timeout max-time)]
      (let [[v p] (a/alts! [in t])] 
        (cond
          (= p t)
          (do
            (a/>! out buf)
            (recur [] (a/timeout max-time)))

          (nil? v)
          (when (seq buf)
            (a/>! out buf)
            (a/close! out))

          (== (count buf) lim-1)
          (do
            (a/>! out (conj buf v))
            (recur [] (a/timeout max-time)))

          :else
          (recur (conj buf v) t))))))

(defn run-notifier! [batch-time batch-size]
  (let [in  (a/chan 100)
        out (a/chan 100)]
    (batch in out batch-time batch-size)
    (notify out)
    in))

(mount/defstate notifier
  :start (let [{:keys [batch-time batch-size]}
               (:notifier env)]
           (run-notifier! batch-time batch-size))
  :stop (a/close! notifier))

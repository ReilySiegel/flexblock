(ns flexblock.notifier
  (:require [clojure.core.async :as a]
            [mount.core :as mount]))

(defn dedupe-notify [batch]
  (distinct batch))

(defn event-printer [event]
  (println event))

(defn notify [in]
  (a/go-loop []
    (when-let [batch (a/<! in)] 
      (println (format "Notified on %d events, compressed from %d."
                       (count (dedupe-notify batch))
                       (count batch)))
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

(defn run-notifier! []
  (let [in  (a/chan 100)
        out (a/chan 100)]
    (batch in out (* 30 60 1000) 100)
    (notify out)
    in))

(mount/defstate notifier
  :start (run-notifier!)
  :stop (a/close! notifier))

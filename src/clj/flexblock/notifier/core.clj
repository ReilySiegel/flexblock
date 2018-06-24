(ns flexblock.notifier.core
  "This namespace collects notifications from the system, batches
  them, and dispatches them to the enabled services."
  (:require [clojure.core.async :as a]
            [mount.core :as mount]
            [flexblock.config :refer [env]]
            [flexblock.notifier.services.core :as services]
            [flexblock.notifier.services.email]))

(defn notify
  "Dispatches each notification in a batch to its service.
  Batches are taken from the channel `in`."
  [in]
  (a/go-loop []
    (when-let [batch (a/<! in)]
      (doseq [event   (distinct batch)
              service services/enabled-services]
        (services/send-notification service event))
      (recur))))

(defn batch
  "Batches notifications.
  Takes an `in` channel, from witch events are taken, and an `out`
  channel, where events are pushed. Completes a batch when either
  `max-time` has passed since the last batch, or `max-count` items are
  ready to be batched."
  [in out max-time max-count]
  (let [lim-1 (dec max-count)]
    (a/go-loop [buf []
                timeout (a/timeout max-time)]
      (let [[val port]
            (a/alts! [in timeout])]
        (cond
          (= port timeout)
          (do
            (a/>! out buf)
            (recur [] (a/timeout max-time)))

          (nil? val)
          (when (seq buf)
            (a/>! out buf)
            (a/close! out))

          (== (count buf) lim-1)
          (do
            (a/> out (conj buf val))
            (recur [] (a/timeout max-time)))

          :else
          (recur (conj buf val) timeout))))))

(defn run-notifier!
  "Sets up the channels for `batch` and `notify`."
  [batch-time batch-size]
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

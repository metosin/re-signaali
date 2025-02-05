(ns re-frame.alpha.uix
  (:require ["react" :as react]
            [re-frame.alpha :as rf]
            [signaali.reactive :as sr]
            [uix.core :as uix]))

(defn use-sub
  ([q]
   (let [[subscribe get-snapshot] (uix/use-memo (fn []
                                                  ;;(prn "in use-memo")
                                                  (let [subscription (sr/with-observer nil
                                                                        #(rf/sub q))
                                                        subscribe (fn [ping-react-that-something-might-have-changed]
                                                                    (let [signal-watcher (reify
                                                                                           sr/ISignalWatcher
                                                                                           (notify-signal-watcher [this is-for-sure signal-source]
                                                                                             (ping-react-that-something-might-have-changed)))]
                                                                      ;;(prn "subscribe")
                                                                      (sr/add-signal-watcher subscription signal-watcher)
                                                                      (fn []
                                                                        ;;(prn "unsubscribe")
                                                                        (sr/remove-signal-watcher subscription signal-watcher))))
                                                        get-snapshot (fn []
                                                                       ;;(prn "in get-snapshot")
                                                                       ;; https://stackoverflow.com/questions/76474940/why-does-the-usesyncexternalstore-example-call-getsnapshot-6-times-on-store
                                                                       @subscription)]
                                                    [subscribe get-snapshot]))
                                                [q])]
     (react/useSyncExternalStore subscribe get-snapshot)))

  ([id q]
   (use-sub (assoc q :re-frame/q id))))

(defn use-subscribe [query-v]
  (use-sub query-v))

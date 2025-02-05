(ns re-frame.uix
  (:require ["react" :as react]
            [re-frame.core :as rf]
            [signaali.reactive :as sr]
            [uix.core :as uix]))

(defn use-reactive-node [reactive-node]
  (let [[subscribe get-snapshot] (react/useMemo (fn []
                                                  ;;(prn "in use-memo")
                                                  (let [subscribe (fn [ping-react-that-something-might-have-changed]
                                                                    (let [signal-watcher (reify
                                                                                           sr/ISignalWatcher
                                                                                           (notify-signal-watcher [this is-for-sure signal-source]
                                                                                             (ping-react-that-something-might-have-changed)))]
                                                                      ;;(prn "subscribe")
                                                                      (sr/add-signal-watcher reactive-node signal-watcher)
                                                                      (fn []
                                                                        ;;(prn "unsubscribe")
                                                                        (sr/remove-signal-watcher reactive-node signal-watcher))))
                                                        get-snapshot (fn []
                                                                       ;;(prn "in get-snapshot")
                                                                       ;; https://stackoverflow.com/questions/76474940/why-does-the-usesyncexternalstore-example-call-getsnapshot-6-times-on-store
                                                                       @reactive-node)]
                                                    [subscribe get-snapshot]))
                                               #js [reactive-node])]
    (react/useSyncExternalStore subscribe get-snapshot)))

(defn use-subscribe [query-v]
  (let [[subscribe get-snapshot] (uix/use-memo (fn []
                                                 ;;(prn "in use-memo")
                                                 (let [subscription (sr/with-observer nil
                                                                       #(rf/subscribe query-v))
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
                                               [query-v])]
    (react/useSyncExternalStore subscribe get-snapshot)))

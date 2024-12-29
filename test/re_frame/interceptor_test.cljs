(ns re-frame.interceptor-test
  (:require [cljs.test :refer-macros [is deftest testing use-fixtures]]
            [reagent.ratom :refer [atom]]
            [re-frame.core :refer [reg-global-interceptor clear-global-interceptor]]
            [re-frame.interceptor :refer [context get-coeffect assoc-effect assoc-coeffect get-effect
                                          update-effect update-coeffect ->interceptor]]
            [re-frame.std-interceptors :refer [debug trim-v path enrich after on-changes
                                               db-handler->interceptor fx-handler->interceptor
                                               inject-global-interceptors]]
            [re-frame.interceptor :as interceptor]
            [re-frame.registrar :as registrar]))

(enable-console-print!)

(defn global-interceptor-fixture
  [f]
  (reg-global-interceptor {:id     :interceptor-test
                           :before (fn [context]
                                     (assoc-coeffect context :global {:direction :before}))
                           :after  (fn [context]
                                     (assoc-coeffect context :global {:direction :after}))})
  (f)
  (clear-global-interceptor :interceptor-test))

(defn error-handler-fixture
  [f]
  (let [original-handler (registrar/get-handler :error :event-handler)]
    (f)
    (registrar/register-handler :error :event-handler original-handler)))

(use-fixtures :once global-interceptor-fixture)
(use-fixtures :each error-handler-fixture)

(deftest test-trim-v
  (let [ctx           (context [:event-id :b :c] [])
        ctx-trimmed   ((:before trim-v) ctx)
        ctx-untrimmed ((:after trim-v) ctx-trimmed)]
    (is (= (get-coeffect ctx-trimmed :event)
           [:b :c]))
    (is (= (get-coeffect ctx-untrimmed :event)
           [:event-id :b :c]))
    (is (= ctx-untrimmed ctx))))

(deftest test-one-level-path
  (let [db   {:showing true :another 1}
        p1   (path [:showing])]   ;; a simple one level path

    (let [b4 (-> (context [] [] db)
                 ((:before p1)))         ;; before
          a (-> b4
                (assoc-effect :db false)
                ((:after p1)))]         ;; after

      (is (= (get-coeffect b4 :db)      ;; test before
             true))
      (is (= (get-effect a :db)         ;; test after
             {:showing false :another 1})))))

(deftest test-two-level-path
  (let [db  {:1 {:2 :target}}
        p  (path [:1 :2])]    ;; a two level path

    (let [b4 (-> (context [] [] db)
                 ((:before p)))]          ;; before

      (is (= (get-coeffect b4 :db))      ;; test before
          :target)

      ;; test #1
      (is (= {:1 {:2 :4}}
             (-> b4
                 (assoc-effect :db :4)    ;; <-- db becomes :4
                 ((:after p))
                 (get-effect :db))))

      ;; test #2 - set db to nil
      (is (= {:1 {:2 nil}}
             (-> b4
                 (assoc-effect :db nil)   ;; <-- db becomes nil
                 ((:after p))
                 (get-effect :db)))))))

(deftest path-with-no-db-returned
  (let [path-interceptor (path :a)]
    (-> (context [] [path-interceptor] {:a 1})
        (interceptor/invoke-interceptors :before)
        interceptor/change-direction
        (interceptor/invoke-interceptors :after)
        (get-effect :db)
        (nil?)                                              ;; We don't expect an effect to be added.
        (is))))

(deftest test-inject-global-interceptors
  (let [forward (-> (context [] [inject-global-interceptors] {:a 1})
                    (interceptor/invoke-interceptors :before))
        _       (is (= {:direction :before} (get-coeffect forward :global)))
        reverse (-> forward
                    interceptor/change-direction
                    (interceptor/invoke-interceptors :after))]
    (is (= {:direction :after} (get-coeffect reverse :global)))))

(deftest test-db-handler-interceptor
  (let [event   [:a :b]

        handler (fn [db v]
                  ;; make sure it was given the right arguments
                  (is (= db :original-db-val))
                  (is (= v event))
                  ;; return a specific value for later checking
                  :new-db-val)

        i1      (db-handler->interceptor handler)
        db      (-> (context event [] :original-db-val)
                    ((:before i1))            ;; calls handler - causing :db in :effects to change
                    (get-effect :db))]
    (is (= db :new-db-val))))

(deftest test-fx-handler-interceptor
  (let [event   [:a :b]
        coeffect {:db 4 :event event :original-event event}
        effect   {:db 5 :dispatch [:a]}

        handler (fn [world v]
                  ;; make sure it was given the right arguments
                  (is (= world coeffect))
                  (is (= v event))

                  ;; return a specific value for later checking
                  effect)

        i1      (fx-handler->interceptor handler)
        e       (-> (context event [] (:db coeffect))
                    ((:before i1))            ;; call the handler
                    (get-effect))]
    (is (= e {:db 5 :dispatch [:a]}))))

(deftest test-on-changes
  (let [change-handler-i  (->  (fn [db v] (assoc db :a 10))
                               db-handler->interceptor)

        no-change-handler-i  (->  (fn [db v] db)
                                  db-handler->interceptor)

        no-db-handler-i (-> (fn [ctx v] {})
                            fx-handler->interceptor)

        change-i   (on-changes + [:c] [:a] [:b])
        orig-db    {:a 0 :b 2}]

    (is (=  {:a 0 :b 2}
            (-> (context [] [] orig-db)
                ((:before no-change-handler-i))   ;; no change to :a and :b
                ((:after change-i))
                (get-effect :db))))
    (is (=  {:a 10 :b 2 :c 12}
            (-> (context [] [] orig-db)
                ((:before change-handler-i))       ;; cause change to :a
                ((:after change-i))
                (get-effect :db))))

    (is (=  ::not-found
            (-> (context [] [] orig-db)
                ((:before no-db-handler-i))       ;; no db effect in context
                ((:after change-i))
                (get-effect :db ::not-found))))))

(deftest test-after
  (testing "when no db effect is returned"
    (let [after-db-val (atom nil)]
      (-> (context [:a :b]
            [(after (fn [db] (reset! after-db-val db)))]
            {:a 1})
          (interceptor/invoke-interceptors :before)
          interceptor/change-direction
          (interceptor/invoke-interceptors :after))
      (is (= @after-db-val {:a 1}))))
  (testing "when a false db effect is returned"
    (let [after-db-val (atom :not-reset)]
      (-> (context [:a :b]
            [(after (fn [db] (reset! after-db-val db)))]
            {:a 2})
          (assoc-effect :db nil)
          (interceptor/invoke-interceptors :before)
          interceptor/change-direction
          (interceptor/invoke-interceptors :after))
      (is (= @after-db-val nil))))
  (testing "when a nil db effect is returned"
    (let [after-db-val (atom :not-reset)]
      (-> (context [:a :b]
            [(after (fn [db] (reset! after-db-val db)))]
            {:a 3})
          (assoc-effect :db false)
          (interceptor/invoke-interceptors :before)
          interceptor/change-direction
          (interceptor/invoke-interceptors :after))
      (is (= @after-db-val false)))))

(deftest test-enrich
  (testing "when no db effect is returned"
    (let [db {:a 1}
          ctx (context [] [] db)
          enrich-interceptor (enrich (fn [db _] db))]
      (is (= ::not-found (get-effect ctx :db ::not-found)))
      (is (get-effect ((:after enrich-interceptor) ctx) :db))
      (is (= db (get-effect ((:after enrich-interceptor) ctx) :db)))))
  (testing "uses db returned by f"
    (let [update-fn (fn [db _] (update db :a inc))
          updater (db-handler->interceptor update-fn)
          ctx (context [] [updater] {:a 1})
          enrich-interceptor (enrich update-fn)
          result (-> ctx
                     ((:before updater))
                     ((:after enrich-interceptor)))]
      (is (= {:a 3} (get-effect result :db)))))
  (testing "uses given db if f returns nil"
    (let [updater (db-handler->interceptor (fn [db _] (update db :a inc)))
          ctx (context [] [updater] {:a 1})
          enrich-interceptor (enrich (fn [_ _] nil))
          result (-> ctx
                     ((:before updater))
                     ((:after enrich-interceptor)))]
      (is (= {:a 2} (get-effect result :db))))))

(deftest test-update-effect
  (let [context {:effects {:db {:a 1}}
                 :coeffects {:db {:a 1}}}]
    (is (= {:effects {:db {:a 2}}
            :coeffects {:db {:a 1}}}
           (update-effect context :db update :a inc)))))

(deftest test-update-coeffect
  (let [context {:effects {:db {:a 1}}
                 :coeffects {:db {:a 1}}}]
    (is (= {:effects {:db {:a 1}}
            :coeffects {:db {:a 2}}}
           (update-coeffect context :db update :a inc)))))

(deftest test-exception->ex-info
  (let [exception->ex-info #'re-frame.interceptor/exception->ex-info ; ref to the private fn
        e (js/Error. "Ooops!")
        interceptor {:id :some-interceptor :before identity}
        result (exception->ex-info e interceptor :after)]
    (is (= {:direction :after
            :interceptor :some-interceptor}
           (ex-data result)))
    (is (= e (ex-cause result)))
    (is (= "Interceptor Exception: Ooops!" (ex-message result)))))

(deftest test-invoke-interceptor-fn
  (let [invoke-interceptor-fn #'re-frame.interceptor/invoke-interceptor-fn ; ref to the private fn
        context {:a 1}
        interceptor {:before #(update % :a inc)}]

    (testing "returns context when there's no direction fn for the interceptor"
      (is (= {:a 1}
             (invoke-interceptor-fn context interceptor :after))))

    (testing "returns result of applying direction fn to context"
      (is (= {:a 2}
             (invoke-interceptor-fn context interceptor :before))))

    (testing "throws original exception to flow when there's a :re-frame.interceptor/original-exception? context key"
      (let [exception (ex-info "Oopsie" {:foo :bar})
            interceptor {:id :throws
                         :before #(throw exception)}
            context {:a 1 :re-frame.interceptor/original-exception? true}]
        (try
          (invoke-interceptor-fn context interceptor :before)
          (is false "interceptor should have thrown")
          (catch :default e
            (is (= exception e))
            (is (= "Oopsie" (ex-message e)))
            (is (= {:foo :bar} (ex-data e)))))))

    (testing "throws via exception->ex-info"
      ;; actual handler doesn't matter here, we just need a registered handler so invoke-exception
      (registrar/register-handler :error :event-handler identity)
      (try
        (let [exception (ex-info "Oopsie" {:foo :bar})
              interceptor {:id :throws
                           :before #(throw exception)}
              context {:a 1}]
          (try
            (invoke-interceptor-fn context interceptor :before)
            (is false "interceptor should have thrown")
            (catch :default e
              (is (not= exception e))
              (is (= "Interceptor Exception: Oopsie" (ex-message e)))
              (is (= exception (ex-cause e)))
              (is (= {:direction :before
                      :interceptor :throws}
                     (ex-data e))))))
        (finally
          (registrar/clear-handlers :error))))))

(deftest test-exceptions
  (let [error-atom (atom nil)
        error-handler (fn [error error-info]
                        (reset! error-atom [error error-info]))
        exception (js/Error "Thrown from interceptor")
        throws-before (->interceptor :id :throws-before
                                     :before (fn [_] (throw exception)))
        interceptors [throws-before]]

    (testing "an exception in an interceptor, without error handler"
      (registrar/register-handler :error :event-handler nil)
      (is (nil? (registrar/get-handler :error :event-handler)))
      (try
        (interceptor/execute [:_] interceptors)
        (is false "interceptor should have thrown")
        (catch :default e
          (is (= "Thrown from interceptor" (ex-message e)))))
      (is (nil? @error-atom)))

    (testing "an exception in an interceptor, with error handler"
      (registrar/register-handler :error :event-handler error-handler)
      (try
        (is (registrar/get-handler :error))
        (interceptor/execute [:_] interceptors)
        (let [[original-error re-frame-error] @error-atom
              {:keys [direction event-v interceptor]} (ex-data re-frame-error)]
          (is (= "Interceptor Exception: Thrown from interceptor"
                 (ex-message re-frame-error)))
          (is (= "Thrown from interceptor"
                 (ex-message original-error)))
          (is (not= (.-stack original-error)
                    (.-stack re-frame-error)))
          (is (= :before direction))
          (is (= [:_] event-v))
          (is (= :throws-before interceptor)))
        (finally
          (registrar/clear-handlers :error))))))

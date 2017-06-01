(ns imas-seamap.specs.spec-utils
  (:require [goog.object :as gobj]
            [cljs.spec.alpha :as s]
            [cljs.spec.test.alpha :as stest]
            [cljs.stacktrace :as st]))

;;; Borrowed from imas-webapps

(defn explain-console
  "prints an explanation to *out*."
  [ed]
  (if ed
    (let [print (fn [& args] (.apply (.-warn js/console) js/console (to-array args)))]
      (doseq [{:keys [path pred val reason via in] :as prob} (::s/problems ed)]
        (when-not (empty? in)
          (print "In:" in ""))
        (print "val: " val)
        (when-not (empty? via)
          (print " fails spec:" (last via)))
        (when-not (empty? path)
          (print " at:" path))
        (print " predicate: " (s/abbrev pred))
        (when reason (print "          " reason))
        (doseq [[k v] prob]
          (when-not (#{:path :pred :val :reason :via :in} k)
            (print k v))))
      (doseq [[k v] ed]
        (when-not (#{::s/problems} k)
          (print k v))))
    (println "Success!")))

(defn conform!
  [v role spec data args]
  (let [conformed (s/conform spec data)]
    (if (= ::s/invalid conformed)
      (let [caller (stest/find-caller
                     (st/parse-stacktrace
                       (stest/get-host-port)
                       (.-stack (js/Error.))
                       (stest/get-env) nil))
            ed (merge (assoc (s/explain-data* spec [role] [] [] data)
                        ::s/args args
                        ::s/failure :instrument)
                      (when caller
                        {::caller caller}))]
        (js/console.error (str "Call to " (pr-str v) " did not conform to spec"))
        (explain-console ed))
      conformed)))


(defn- spec-checking-fn
  [v f fn-spec]
  (let [fn-spec (@#'s/maybe-spec fn-spec)]
    (doto
      (fn
        [& args]
        (if stest/*instrument-enabled*
          (stest/with-instrument-disabled
            (when (:args fn-spec) (conform! v :args (:args fn-spec) args args))
            (binding [stest/*instrument-enabled* true]
              (apply f args)))
          (apply f args)))
      (gobj/extend f))))

(defn patch-spec-checking-fn []
  (set! cljs.spec.test/spec-checking-fn spec-checking-fn))

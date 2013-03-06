(ns clojure.tools.test-trace
  "Tests for tools.trace."
  [:use [clojure.test] [clojure.tools.trace]]
  [:require [clojure.string :as s]])

(defn ^{:private true}
  cleanup
  "Remove variable output from the trace output and replace end of lines by |"
  [s]
  (s/replace (s/replace s #"t[0-9]+:" "t:#") #"\n" "|"))

(deftrace fn-a
  "fn-a Doc string"
  [a b c]
  (+ a b c))

(deftrace fn-b
  [a b c]
  (+ a b c))

(defn fn-let
  [a b c]
  (trace-forms (let [ d (/ 3 0)] d)))

(defn fn-fn
  [a b c]
  (trace-forms ((fn [] (let [ d (/ 3 0)] d)))))

(deftest test-with-docstring
  (is (= (.endsWith ^String (cleanup (:doc (meta (var fn-a)))) "fn-a Doc string") true)
      (is (= (cleanup (with-out-str (fn-a 1 2 3))) "TRACE t:# (fn-a 1 2 3)|TRACE t:# => 6|"))))

(deftest test-no-docstring
  (is (= (cleanup (with-out-str (fn-b 1 2 3))) "TRACE t:# (fn-b 1 2 3)|TRACE t:# => 6|")))

(deftest test-trace-form
  (is (thrown-with-msg? ArithmeticException #"Divide by zero\n  Form failed: \(/ 9 a\)\n  Form failed: \(let\* \[a 0 b \(/ 9 a\)\] b\)\n  Form failed: \(let \[a 0 b \(/ 9 a\)\] b\)"
        (trace-forms (let [a 0 b (/ 9 a)] b)))))

(deftest test-fn-form
  (is (thrown-with-msg? ArithmeticException #"Divide by zero\n  Form failed: \(\(fn \[\] \(let \[d \(/ 3 0\)\] d\)\)\)"
        (fn-fn 1 2 3))))

(deftest test-maps
  (is (thrown-with-msg? ArithmeticException #"Divide by zero\n  Form failed: \(/ 3 0\)\n  Form failed: \{:a 1, :b \(/ 3 0\)\}"
        (trace-forms {:a 1 :b (/ 3 0)}))))

(def trace-ns-test-namespace (create-ns 'trace.test.namesp))

(binding [*ns* trace-ns-test-namespace]
  (eval '(clojure.core/refer-clojure))
  (eval '(defn foo [] :foo))
  (eval '(defn bar [] (foo))))

(deftest test-trace-foo
  (trace-vars trace.test.namesp/bar)
  (is (= (cleanup (with-out-str (trace.test.namesp/bar)))
         "TRACE t:# (trace.test.namesp/bar)|TRACE t:# => :foo|"))
  (untrace-vars trace.test.namesp/bar)
  (is (= (cleanup (with-out-str (trace.test.namesp/bar))) "")))

(deftest test-trace-all
  (trace-vars trace.test.namesp/bar trace.test.namesp/foo)
  (is (= (cleanup (with-out-str (trace.test.namesp/bar)))
         "TRACE t:# (trace.test.namesp/bar)|TRACE t:# | (trace.test.namesp/foo)|TRACE t:# | => :foo|TRACE t:# => :foo|" ))
  (untrace-vars trace.test.namesp/bar trace.test.namesp/foo)
  (is (= (cleanup (with-out-str (trace.test.namesp/bar))) "")))

(deftest test-trace-ns
  (trace-ns trace-ns-test-namespace)
  (is (= (cleanup (with-out-str (trace.test.namesp/bar)))
         "TRACE t:# (trace.test.namesp/bar)|TRACE t:# | (trace.test.namesp/foo)|TRACE t:# | => :foo|TRACE t:# => :foo|"))
  (untrace-ns trace-ns-test-namespace)
  (is (= (cleanup (with-out-str (trace.test.namesp/bar))) "")))

(deftest istraced
  (is (not (traced? 'trace.test.namesp/bar)))
  (trace-vars trace.test.namesp/bar)
  (is (traced? 'trace.test.namesp/bar))
  (untrace-vars trace.test.namesp/bar))

(deftest istraceable
  (is (traceable? 'trace.test.namesp/bar)))

(defn collect-names-from-trace "utility function to walk trace" [collected-names trace-result]
  (if
      (list? trace-result) (map (partial work collected-names)  trace-result)
      (if (:children trace-result)
        (conj collected-names (collect-names-from-trace collected-names (:children trace-result))  (:name trace-result) )
        (conj collected-names (:name trace-result)))))

(defn ^{:dynamic true} level-2 [] "a")
(defn ^{:dynamic true} level-1a [] (level-2))
(defn ^{:dynamic true} level-1b [] (do (level-2) (level-2)))
(defn ^{:dynamic true} level-0 [] (do (level-1a) (level-1b) (level-2)))

(deftest isrecorded
  (is (= '(level-2 level-2 level-1b level-2 level-1a level-0)
         (->> (trace-record [level-0 level-1a level-1b level-2] (level-0))
              :trace
              first
              (collect-names-from-trace [])
              flatten))))

(run-tests)
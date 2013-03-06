(ns clojure.tools.use
  (require [clojure.tools.trace :as trace]
           [clojure.tools.combine-trace :as combine-trace]))

(defn ^{:dynamic true} a [x] x)
(defn ^{:dynamic true} b [x] x)
(defn ^{:dynamic true} c [x] x)

                                        ;(require '(clojure [ stacktrace :as st]))
(defn ^{:dynamic true} decision [y]
  (cond
   (< y 10) (a "less than 10")
   (= y 10) (b "equal to 10")
   :else    (c "default")))

(defn trace-decision [x]
  (trace/trace-record [a b c decision] (decision x)))

(defn enumerate []
  (let [f (trace-decision -10)
        s (trace-decision 10)
        t  (trace-decision 1000)]
    ( :trace t )))

(def b1 {:children '({:id 32936250, :name "a", :args '("less than 10"), :result "less than 10", :trace-depth 1}), :id 21190344, :name "decision" :args '(-10), :result "less than 10", :trace-depth 0})

(def b2 {:children '({:id 25246752, :name "c" :args '("default"), :result "default", :trace-depth 1}), :id 3669311, :name "decision" :args '(1000), :result "default", :trace-depth 0})

(combine-trace/merge-trees (combine-trace/combine-tree-ize b1) b2)

(comment
                                        ;  { :root : { :outlookrain { :playNotWindy {}  :dontPlayWindy {}}
                                        ;            :outlooksunny { :playNotHumid {} :dontPlayNotHumid {}}
                                        ;} }
  )

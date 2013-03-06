(ns clojure.tools.combine-trace
  (require [clojure.walk :as walk]))


(defn ^{:dynamic true} find-distinct-subtree
  "merge tree nodes only have 0 or 1 children"
  [path ref-kids merge-tree]                                    
  (let [name-as-keyword (keyword (:name merge-tree))
        matching-child (get ref-kids name-as-keyword )]
    (if matching-child
      (find-distinct-subtree (conj path name-as-keyword)  matching-child (first (:children merge-tree)))
      [path merge-tree])))

(defn combine-tree-ize-node [x]
  (if (map? x) (if (:children x)
                 {(keyword (:name x)) (first (:children x))}
                 {(keyword (:name x)) nil})
      x))

(defn combine-tree-ize [trace-tree]
  (walk/prewalk combine-tree-ize-node trace-tree))

(defn merge-trees [ref-tree merge-tree]
  (let [[path distinct-subtree] (find-distinct-subtree []  ref-tree merge-tree)
        [sub-tree-kw sub-tree-children] (-> (combine-tree-ize distinct-subtree) seq first)]
    (update-in ref-tree path assoc sub-tree-kw sub-tree-children)))

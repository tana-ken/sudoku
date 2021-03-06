(ns sudoku.logic.interactive
  (:import
   org.slf4j.Logger
   org.slf4j.LoggerFactory)
  (:use
   [clojure.contrib.seq-utils :only (indexed)]
   [clojure.set :only (difference union)]
  )
;;  (:require :reload-all sudoku.logic.utils)
  )

(def init (map #(Character/digit % 10)
; "003800000000400280009060003090000130200000009035000060100070400076005000000001600"
  "076005230000600005580000000090850407000000000801046090000000029700001000025300870"
	       ))
     
(def logger (LoggerFactory/getLogger (str *ns*)))

(defn not-nil? [x]
  (not (nil? x)))

(defn remove-nil [coll]
  (filter #(not-nil? %) coll))

(defn not-zero? [x]
  (not (zero? x)))

(defn zero-coll? [coll]
  (zero? (count coll)))

(defn box-criteria [x]
  (if (< x 3)
    0
    (if (< x 6)
      1
      2)))

(defn calc-box [row col]
  (+ (* 3 (box-criteria row)) (box-criteria col)))

(defn val-list-to-recs [coll]
  (for [[idx elt] (indexed coll)]
    (let [row (quot idx 9) col (rem idx 9)]
      {:row row
       :col col
       :box (calc-box row col)
       :value (if (zero? elt) nil elt)})))

(defn sort-recs [recs]
  (sort
   #(let [row (compare (%1 :row) (%2 :row))]
      (if (zero? row) (compare (%1 :col) (%2 :col)) row))
   recs))

(defn recs-to-val-list [recs]
  (map #(% :value) (sort-recs recs)))

(defn swap-list-val [coll idx val]
  (map #(if (= idx (% 0)) val (% 1)) (indexed coll)))

(defn select-recs [recs ope key cra]
  (filter #(ope (% key) cra) recs)) 

(defn select-recs-not [recs ope key cra]
  (filter #(not (ope (% key) cra)) recs))

(defn select-rec [recs row col]
  (first (filter #(and (= (% :row) row) (= (% :col) col)) recs)))

(defn select-sub-relative-recs [recs row col]
  (select-recs-not 
   (into (select-recs recs = :row row) (select-recs recs = :col col))
   = :box (calc-box row col)))

(defn select-relative-recs [recs row col]
  (into
   (select-recs recs = :box (calc-box row col))
   (select-sub-relative-recs recs row col)))

(def set19 (set (range 1 10)))

(def history (ref '()))
(defn cons-history [x]
  (dosync (ref-set history (cons x @history))))
(defn rest-history []
  (dosync (ref-set history (rest @history))))

(defn swap-recs [to-recs from-recs]
  (for [from-rec from-recs]
    (let [rec (select-rec to-recs (from-rec :row) (from-rec :col))]
      (if (zero-coll? rec) from-rec rec))))

(defn fill-cnd-vals [recs row col]
  (difference set19
  (set (filter #(number? %) (recs-to-val-list
    (select-relative-recs recs row col))))))

(defn select-cnd-recs [recs]
  (for [cnd-rec (filter #(not (number? (% :value))) recs)]
    (let [row (cnd-rec :row) col (cnd-rec :col) val (cnd-rec :value)]
      (assoc cnd-rec :value (if (nil? val) (fill-cnd-vals recs row col) val)))))

(defn union-cnd-vals [recs]
  (reduce union (recs-to-val-list (select-cnd-recs recs))))

(defn search-cnd-difference [sub-recs key no]
  (difference (union-cnd-vals (select-recs sub-recs = key no)) (union-cnd-vals (select-recs-not sub-recs = key no))))

(defn detect-lock-cnd-in [recs]
  (reduce #(into %1 %2)
     (for [before
       (for [[idx elt] (indexed (map #(select-cnd-recs (select-recs recs = :box %)) (range 9)))
	:when (#(not (zero-coll? %)) elt)]
     (for [key '(:row :col) j (range 0 3)]
       (let [no (if (= :row key) (+ (* (quot idx 3) 3) j) (+ (* (rem idx 3) 3) j))]
	 [idx key no (search-cnd-difference elt key no)])))]
     (filter #(not (zero-coll? (% 3))) before))))

(defn select-sub-cnd-recs-in [recs reduce-key]
  (select-recs-not (select-recs (select-cnd-recs recs) = (reduce-key 1) (reduce-key 2)) = :box (reduce-key 0))) 

(defn reduce-cnd-vals-sub [sub-cnd-recs reduce-set]
  (for [rec sub-cnd-recs]
    (assoc rec :value (difference (rec :value) reduce-set))))

(defn select-reduced-recs "out: reduced-recs" [recs reduce-key]
  (swap-recs
    (reduce-cnd-vals-sub (select-sub-cnd-recs-in recs reduce-key) (reduce-key 3))
    recs))

(defn reduce-cnd-vals "" [recs]
  (let [reduce-keys (detect-lock-cnd-in recs)]
     (reduce select-reduced-recs recs reduce-keys)))

(defn select-naked-single-recs "todo" [recs]
  (for [rec (filter #(= (count (% :value)) 1) (select-cnd-recs recs))]
    (assoc rec :value (first (rec :value)))))

(defn search-hidden-single [recs key no]
  (first 
    (let [cnd-vals-list (recs-to-val-list (select-recs (select-cnd-recs recs) = key no))]
      (filter (fn [i] (= 1 (count (filter #(% i) cnd-vals-list)))) (reduce union cnd-vals-list)))))

(defn detect-rec "todo" [recs key no val]
  (let [cnd-rec (first (filter #(not-nil? ((% :value) val)) (select-recs (select-cnd-recs recs) = key no)))]
    (assoc cnd-rec :value val)))

(defn select-hidden-single-recs [recs]
  (into '() (set (remove-nil
    (for [key '(:row :col :box) no (range 9)]
      (let [val (search-hidden-single recs key no)]
        (if (not-nil? val) (detect-rec recs key no val))))))))

(defn caller [recs]
  (let [result (into (select-naked-single-recs recs) (select-hidden-single-recs recs))]
    (if (zero-coll? result) (swap-recs (select-cnd-recs recs) recs) (recur (swap-recs result recs)))))

(defn all-true? [coll]
  (if (zero-coll? coll)
    true
    (if (true? (first coll))
      (recur (rest coll))
      false)))

(defn same-recs? [to-recs from-recs]
  (all-true?
    (for [row (range 9) col (range 9)]
    (= ((select-rec to-recs row col) :value) ((select-rec from-recs row col) :value)))))

(defn caller-caller [recs]
  (let [r-recs (reduce-cnd-vals recs)]
    (if (same-recs? r-recs recs)
      recs
      (recur (caller r-recs)))))
    
(defn get-result []
  (recs-to-val-list (caller (val-list-to-recs init))))

(def tmp (caller (val-list-to-recs init)))
(defn tmp2 [] (recs-to-val-list tmp))
(def tp (reduce-cnd-vals tmp))
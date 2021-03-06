(ns sudoku.sudoku
  (:use
    [clojure.set]
    [clojure.contrib.seq-utils :only (indexed)]))

(def initial '(
  (6 8 0 4 0 0 0 9 5)
  (0 0 0 0 0 0 0 8 6)
  (0 0 1 0 0 0 0 2 0)
  (4 0 0 1 8 0 0 0 0)
  (0 0 0 0 9 0 0 0 0)
  (0 0 0 0 5 4 0 0 7)
  (0 7 0 0 0 0 4 0 0)
  (8 9 0 0 0 0 0 0 0)
  (3 4 0 0 0 2 0 6 9)))

;  (0 0 3 8 0 0 0 0 0)
;  (0 0 0 4 0 0 2 8 0)
;  (0 0 9 0 6 0 0 0 3)
;  (0 9 0 0 0 0 1 3 0)
;  (2 0 0 0 0 0 0 0 9)
;  (0 3 5 0 0 0 0 6 0)
;  (1 0 0 0 7 0 4 0 0)
;  (0 7 6 0 0 5 0 0 0)
;  (0 0 0 0 0 1 6 0 0)))

;  (0 0 0 0 0 0 0 0 0)
;  (0 0 0 0 0 0 0 0 0)
;  (0 0 0 0 0 0 0 0 0)
;  (0 0 0 0 0 0 0 0 0)
;  (0 0 0 0 0 0 0 0 0)
;  (0 0 0 0 0 0 0 0 0)
;  (0 0 0 0 0 0 0 0 0)
;  (0 0 0 0 0 0 0 0 0)
;  (0 0 0 0 0 0 0 0 0)

(defn not-nil? [value]
  (not (nil? value)))

(defn remove-nil [seq]
  (filter #(not-nil? %) seq))

(defn zero-seq? [seq]
  (zero? (count seq)))

(defn area-criteria [i]
  (if (< i 3)
    0
    (if (< i 6)
      1
      2)))

(defn calc-area-num [row col]
  (+ (* 3 (area-criteria row)) (area-criteria col)))

(defn create-recs [lists]
  (remove-nil
    (for [row (range 9) col (range 9)]
      (let [value (nth (nth lists row) col)]
	(if (not (zero? value))
          {:row row
           :col col
           :area (calc-area-num row col)
           :value value})))))
(defn build-recs [coll]
  (remove-nil
    (for [elem (indexed coll)]
      (let [index (elem 0) value (elem 1)]
        (if (not (zero? value))
	  (let [row (quot index 9) col (rem index 9)]
	    {:row row
	     :col col
	     :area (calc-area-num row col)
	     :value value}))))))
  
(defn find-recs [keytype num recs]
  (filter #(= (keytype %) num) recs))

(defn find-rec [row col recs]
  (first (find-recs :row row (find-recs :col col recs))))

(defn print-recs [recs]
  (if (not (zero-seq? recs))
    (do (println (first recs))
	(recur (rest recs)))))

(defn marge-recs [old-recs new-recs]
  (remove-nil
    (for [row (range 9) col (range 9)]
      (let [new-rec (find-rec row col new-recs)]
	(if (nil? new-rec)
	  (find-rec row col old-recs)
	  new-rec)))))

(defn find-relative-recs "area double counted" [row col recs]
  (into (find-recs :area (calc-area-num row col) recs)
	(into (find-recs :row row recs) (find-recs :col col recs))))

(defn create-value-set [recs]
  (loop [src recs dst '()]
    (if (zero-seq? src)
      (set dst)
      (recur (rest src) (cons (:value (first src)) dst)))))
   
(def set-1-9 (set (for [i (range 1 10)] i)))

(defn find-probable-recs [recs]
  (remove-nil
    (for [row (range 9) col (range 9)]
      (if (nil? (find-rec row col recs))
	{:row row
	 :col col
	 :area (calc-area-num row col)
	 :value
	   (difference
	     set-1-9
	     (create-value-set (find-relative-recs row col recs)))}))))

(defn find-fixed-recs-p [recs]
  (for [rec (filter #(= (count (:value %)) 1) (find-probable-recs recs))]
    {:row (:row rec)
     :col (:col rec)
     :area (:area rec)
     :value (first (:value rec))}))

(defn include? [set value]
  (not (nil? (set value))))

(defn count-values [recs]
  (for [num (range 9)]
    (count (filter #(include? (:value %) num) recs))))

(defn find-fix-value [seq]
  (loop [src seq i 0]
    (if (zero-seq? src)
      nil
      (if (= 1 (first src))
	i
	(recur (rest src) (inc i))))))

(defn find-rec-by-value [value recs]
  (first (filter #(include? (:value %) value) recs)))

(defn find-fixed-recs-n [recs]
  (remove-nil
    (for [keytype [:row :col :area] num (range 9)]
      (let [sub-recs (find-recs keytype num (find-probable-recs recs))]
	(if (not-nil? sub-recs)
	  (let [value (find-fix-value	(count-values sub-recs))]
	    (if (not-nil? value)
	      (let [rec (find-rec-by-value value sub-recs)]
		{:row (:row rec)
		 :col (:col rec)
		 :area (:area rec)
		 :value value}))))))))

(defn main-proc [recs]
  (let [add-recs (into (find-fixed-recs-n recs) (find-fixed-recs-p recs))]
    (if (or (nil? add-recs) (zero-seq? add-recs))
      recs
      (recur (marge-recs recs add-recs)))))

(defn sort-recs [recs]
  (for [row (range 9) col (range 9)]
    (find-rec row col recs)))

(def result
  (let [calced (main-proc (create-recs initial))] 
    (sort-recs (into calced (find-probable-recs calced)))))

(defn print-result [recs]
  (loop [row 0]
    (if (> 9 row)
      (do
	(println
	  (let [cols (find-recs :row row recs)]
	    (for [col cols]
	      (:value col))))
	(recur (inc row))))))


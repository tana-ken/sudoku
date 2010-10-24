(ns sudoku.core
  (:import
    org.slf4j.Logger
    org.slf4j.LoggerFactory)
  (:use
    [clojure.contrib.seq-utils :only (indexed)]
    clojure.contrib.json
    compojure.core
    hiccup.core,hiccup.form-helpers
    hiccup.page-helpers
    ring.adapter.jetty
    ring.middleware.params
    ring.middleware.session
    ring.middleware.file-info
    ring.middleware.file)
  (:require :reload-all sudoku.sudoku, sudoku.logic.interactive))

(def logger (LoggerFactory/getLogger (str *ns*)))

(defmacro espy [expr]
  `(let [a# ~expr]
     (do (.info logger (str '~expr " => " a#)) a#)))

(defn include-all-js []
  (html
    (include-js "/jquery-1.4.2.js")
    (include-js "/jquery.form.js")
    (include-js "/sudoku.js")))

(defn create-cells [number]
  [:div {:class "cell" :id (str number)}])
  
(defn display []
  (html
    (include-all-js)
    (include-css "/sudoku.css")
    [:h1 "sudoku"]
    (form-to
      {:id "init"}
      [:post "/init"]
      "init"
      [:br]
      (text-field :init)
      (submit-button "send"))
    (form-to
      {:id "hint1"}
      [:post "/hint1"]
      "hint1"
      (submit-button "hint1"))

    (form-to
      {:id "hint2"}
      [:post "/hint2"]
      "hint2"
      (submit-button "hint2"))

    [:div {:id "base" :class "main"}
      (map create-cells (range 0 81))
    ]

  ))


(defn trans-zero [seq]
  (for [i seq]
    (if (zero? i) (range 1 10) i)))

(defn handle-init [init session]
  (let [init-list (for [c init] (Character/digit c 10))]
    {:body (json-str (trans-zero init-list))
     :session {:init-list init-list}}))

(defn result [value-list]
     (let [calced (sudoku.sudoku/main-proc (sudoku.sudoku/build-recs value-list))]
       (sudoku.sudoku/sort-recs (into calced (sudoku.sudoku/find-probable-recs calced)))))

(defn handle-hint1 [params session]
  {:body (json-str
           (for [rec (result (session :init-list))]
	     (rec :value)))
   :session session})

(defn handle-hint2 [params session]
  (json-str (sudoku.logic.interactive/tmp2)))

(defn swap-coll [coll index value]
  (for [elem (indexed coll)]
    (if (= (elem 0) index) value (elem 1))))
    
(defn handle-cell [params session]
  (let [init-list (espy (swap-coll (session :init-list) (Integer/parseInt (params "index")) (Integer/parseInt (params "value"))))] 
    {:body (json-str
             (for [rec (result init-list)]
	       (rec :value)))
   :session {:init-list init-list}}))

(defroutes myroutes
  (GET "/" [] (display))
  (POST "/init" {session :session params :params} (handle-init (params "init") session))
  (POST "/hint1" {session :session params :params} (handle-hint1 params session))
  (POST "/hint2" {session :session params :params} (handle-hint2 params session))
  (POST "/cell" {session :session params :params} (handle-cell params session)))

(wrap! myroutes :session)

(def app
  (->
    #'myroutes
    (wrap-file "files")
    wrap-file-info))

(defonce server
  (run-jetty
    app
    {:join? false :port 8080}))

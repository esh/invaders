(ns universe
	(:use [clojure.contrib.sql :only (with-connection with-query-results)])
	(:use [clojure.contrib.json.read])
	(:use [clojure.contrib.json.write])
	(:use [clojure.walk])
	(:require [amqp]))

(. Class (forName "org.sqlite.JDBC"))

(def *db* {:classname "org.sqlite.JDBC"
	   :subprotocol "sqlite"
	   :subname "universe.db"})

(def *items* (with-connection *db* (with-query-results results ["select name from items"]
	(reduce (fn [items row] (assoc items (keyword (:name row)) 0)) {} results))))

(def *ship-types* (with-connection *db* (with-query-results results ["select * from ship_types"]
	(reduce (fn [ships row] (assoc ships (keyword (:name row)) row)) {} results))))

(def *possessions-atom*  
	(atom (with-connection *db* 
		(with-query-results results ["select owner, item, sum(qty) as qty, max(timestamp) as timestamp from possessions group by owner, item"]
			(reduce (fn [coll val]
				(let [user (keyword (:owner val))
				      item (keyword (:item val))
				      qty (ref (:qty val))
				      old (if (contains? coll user) (user coll) {})]
					(assoc coll user (assoc old item qty))))
		{} results))))) 
				
(defn load-table [table-name]
	(with-connection *db* (with-query-results results [(str "select * from " table-name)] 
		(reduce (fn [coll val] (conj coll (assoc val :type table-name))) [] results))))

(def *universe-ref* 
	(ref (reduce (fn [coll val]
			(let [x (:x val)
			      y (:y val)
			      old (if (contains? coll [x y]) (get coll [x y]) [])] 
				(assoc coll [x y] (conj old val))))
		{}
		(into (load-table "resources") (load-table "ships")))))

(defn mine-resources []
	(filter #(not (nil? %))
		(map (fn [sector] 
			(let [resources (filter #(= (:type %) "resources") sector)
			      owner (first (filter #(= (:type %) "ships") sector))]
				(if (empty? owner)
					nil
					{(keyword (:owner owner)) resources})))	
		     (vals @*universe-ref*))))


(defmulti dispatch #(keyword (:action %)))

(defmethod dispatch :possessions [msg]
	(let [user (:user msg)
	      possessions ((keyword user) @*possessions-atom*)]
		(assoc (zipmap
				(keys possessions)
				(map deref (vals possessions)))
			:type "possessions")))

(defmethod dispatch :universe [msg]
	(let [user (:user msg)
      	      universe @*universe-ref*]
		(assoc universe :type "universe")))


;listen to universe
(let [conn (amqp/connect "localhost" 5672 "guest" "guest" "/")
      chan (amqp/create-channel conn "ex" "topic")]
	(amqp/declare-queue chan "universe")
	(amqp/bind-queue chan "universe" "ex" "universe")
	(amqp/subscribe chan "universe"
		(fn [msg]
			(let [msg (keywordize-keys (read-json-string msg))
			      user (:user msg)
			      res (dispatch msg)]
				(println res)
				(amqp/publish chan "ex" (str "client." user) (json-str res)))))) 

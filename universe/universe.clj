(ns universe
	(:use [clojure.contrib.sql :only (with-connection with-query-results)])
	(:require [amqp]))

(. Class (forName "org.sqlite.JDBC"))

(def *universe-db* {:classname "org.sqlite.JDBC"
	   :subprotocol "sqlite"
	   :subname "../db/universe.db"})

(def *user-db* {:classname "org.sqlite.JDBC"
	:subprotocol "sqlite"
	:subname "../db/clients.db"})

(def *users* (with-connection *user-db* (with-query-results results ["select user from clients"]
	(reduce (fn [items row] (conj items (keyword (:user row)))) [] results)))) 

(def *items* (with-connection *universe-db* (with-query-results results ["select name from items"]
	(reduce (fn [items row] (conj items (keyword (:name row)))) [] results))))

(def *ship-types* (with-connection *universe-db* (with-query-results results ["select * from ship_types"]
	(reduce (fn [ships row] (assoc ships (keyword (:name row)) row)) {} results))))

(def *possessions-atom*
	(atom (zipmap
		*users*
		(take
			(count *users*)
			(repeatedly #(zipmap
					*items*
					(map ref (replicate (count *items*) 0))))))))

(defn update-possessions [user type n]
	(dosync (let [val (type (user @*possessions-atom*))]
		(commute val + n)))) 

;load the universe db into memory
(with-connection *universe-db* 
	(with-query-results results ["select owner, item, sum(qty) as qty, max(timestamp) as timestamp from possessions group by owner, item"]
		(doseq [r results]
			(let [user (keyword (:owner r))
			      type (keyword (:item r))
			      n (:qty r)]
				(update-possessions user type n)))))
	
(defn load-table [table-name]
	(with-connection *universe-db* (with-query-results results [(str "select * from " table-name)] 
		(reduce (fn [coll val] (conj coll (assoc val :type table-name))) [] results))))

(def *universe-atom* 
	(atom (let [universe (reduce (fn [coll val]
					(let [x (:x val)
			      		      y (:y val)
			      			old (if (contains? coll [x y]) (get coll [x y]) [])] 
						(assoc coll [x y] (conj old val))))
				     {}
				     (into (load-table "resources") (load-table "ships")))]
		(zipmap (keys universe) (map ref (vals universe))))))

(defn mine-resources []
	(let [deltas (filter
			#(not (nil? %))
			(map (fn [sector] 
				(let [resources (filter #(= (:type %) "resources") sector)
			      	      owner (first (filter #(= (:type %) "ships") sector))]
					(if (empty? owner)
						nil
						{(keyword (:owner owner)) resources})))	
		     	     (map deref (vals @*universe-atom*))))]
		(doseq [i deltas]
			(let [user (first (keys i)) resources (first (vals i))] 
				(dosync (println user resources))))))

(defn get-universe []
	(dosync (let [universe @*universe-atom*]
      	      		(assoc (zipmap
				(keys universe)
				(map deref (vals universe)))
			:type "universe"))))

(defn get-possessions [user]
	(dosync (let [possessions ((keyword user) @*possessions-atom*)]
	      		(assoc (zipmap
				(keys possessions)
				(map deref (vals possessions)))
			:type "possessions"))))

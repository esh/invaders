(ns universe
	(:use [clojure.contrib.sql]))

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

(def *ships-atom*
	(atom (with-connection *universe-db* (with-query-results results ["select * from ships"]
		(doall (map #(ref (assoc % :type "ships")) results))))))

(def *resources-atom*
	(with-connection *universe-db* (with-query-results results ["select * from resources"] 
		(atom (reduce (partial merge-with into) (doall (map (fn [r] {[(:x r) (:y r)] [r]}) results)))))))
 
(defn get-online-users []
	(with-connection *user-db* (with-query-results results ["select user from clients where status='online'"]
		(reduce (fn [items row] (conj items (:user row))) [] results)))) 
 
(defn get-universe []
	(dosync 
		(let [ships (map deref @*ships-atom*)
		      k (map (fn [r] [(:x r) (:y r)]) ships)
		      ships (zipmap k (map (fn [r] [r]) ships))]
			(merge-with into @*resources-atom* ships))))

(defn get-possessions [user]
	(dosync (let [possessions ((keyword user) @*possessions-atom*)]
			(zipmap
				(keys possessions)
				(map deref (vals possessions))))))

(defn update-possessions [user type n]
	(dosync (let [user (keyword user)
		      type (keyword type)
		      val (type (user @*possessions-atom*))]
			(commute val + n))))

;(defn step-ship [pos] 
;       (dosync (let [ship (get @*ships-ref* pos)
;       	      res (get @*resources-ref* pos)]
;       		(if (not (nil? res))
;       			(let [owner (:owner ship)
;       			      item (:item res)
;       	      		      yield (:yield res)
;       			      timestamp (. System currentTimeMillis)]
;       				(update-possessions owner item yield)		
;       				(with-connection *universe-db*
;       					(insert-rows "possessions" [owner item yield timestamp])))))))

(with-connection *universe-db* 
	(with-query-results results ["select owner, item, sum(qty) as qty, max(timestamp) as timestamp from possessions group by owner, item"]
		(doseq [r results]
			(let [user (keyword (:owner r))
			      type (keyword (:item r))
			      n (:qty r)]
				(update-possessions user type n)))))

(ns universe
	(:use [clojure.contrib.sql]))

(. Class (forName "org.sqlite.JDBC"))

(def *universe-db* {:classname "org.sqlite.JDBC"
	   :subprotocol "sqlite"
	   :subname "../db/universe.db"})

(def *user-db* {:classname "org.sqlite.JDBC"
	:subprotocol "sqlite"
	:subname "../db/clients.db"})

(defn table-to-map [db table-name]
	(with-connection db (with-query-results results [(str "select * from " table-name)] 
		(let [v (map (fn [r] (assoc r :type table-name)) results)
		      k (map (fn [r] [(:x r) (:y r)]) v)
		      v (map (fn [r] (zipmap (rest (rest (keys r))) (rest (rest (vals r))))) v)]
			(zipmap k v)))))

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

(def *ships-ref* (ref (table-to-map *universe-db* "ships")))

(def *resources-ref* (ref (table-to-map *universe-db* "resources")))

(defn get-online-users []
	(with-connection *user-db* (with-query-results results ["select user from clients where status='online'"]
		(reduce (fn [items row] (conj items (:user row))) [] results)))) 
 
(defn get-universe []
	(dosync (merge-with
			(fn [a b]
				(let [a (if (list? a) a [a])
				      b (if (list? b) b [b])]
					(into a b))) 
					@*resources-ref* @*ships-ref*)))

(defn get-possessions [user]
	(dosync (let [possessions ((keyword user) @*possessions-atom*)]
			(zipmap
				(keys possessions)
				(map deref (vals possessions))))))

(defn update-possessions [user type n]
	(dosync (let [val (type (user @*possessions-atom*))]
			(commute val + n))))

(defn mine-resources [] 
	(let [deltas (filter
			#(and (contains? % :owner) (contains? % :item))
			(vals (merge-with
				(fn [a b] {:owner (:owner b) :item (:item a) :yield (:yield a)})
				@*resources-ref* @*ships-ref*)))] 
		(doseq [d deltas]
			(let [owner (keyword (:owner d))
                              item (keyword (:item d))
			      yield (:yield d)] 
				(update-possessions owner item yield)))
		(with-connection *universe-db*
			(transaction (doseq [d deltas]
				(let [owner (:owner d)
				      item (:item d)
				      yield (:yield d)
				      timestamp (. System currentTimeMillis)]
					(insert-rows "possessions" [owner item yield timestamp])))))))

(defn move-ship [source dest]
	(dosync (if (contains? @*ships-ref* source)
			(let [ship (get @*ships-ref* source)]
				(ref-set *ships-ref*
					(assoc (dissoc @*ships-ref* source) dest ship))))))

(with-connection *universe-db* 
	(with-query-results results ["select owner, item, sum(qty) as qty, max(timestamp) as timestamp from possessions group by owner, item"]
		(doseq [r results]
			(let [user (keyword (:owner r))
			      type (keyword (:item r))
			      n (:qty r)]
				(update-possessions user type n)))))

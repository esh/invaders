(ns universe
	(:use [clojure.contrib.sql]))

(. Class (forName "org.sqlite.JDBC"))

(def *universe-db* {:classname "org.sqlite.JDBC"
	   :subprotocol "sqlite"
	   :subname "../db/universe.db"})

(def *user-db* {:classname "org.sqlite.JDBC"
	:subprotocol "sqlite"
	:subname "../db/clients.db"})

(def *possessions-atom*
	(let [users (with-connection *user-db* (with-query-results results ["select user from clients"] 
			(doall (map #(keyword (:user %)) results))))
	      items (with-connection *universe-db* (with-query-results results ["select name from items"] 
			(doall (map #(keyword (:name %)) results))))]
		(atom (zipmap
				users
				(take
					(count users)
					(repeatedly #(zipmap
						items
						(map ref (replicate (count items) 0)))))))))

(def *ships-atom*
	(atom (with-connection *universe-db* (with-query-results results ["select * from ships"]
		(doall (map #(ref (assoc % :type "ships")) results))))))

(def *resources-atom*
	(with-connection *universe-db* (with-query-results results ["select * from resources"] 
		(atom (reduce (partial merge-with into) (doall (map (fn [r] {[(:x r) (:y r)] [r]}) results)))))))
 
(defn get-online-users []
	(with-connection *user-db* (with-query-results results ["select user from clients where status='online'"]
		(apply hash-set (map #(:user %) results))))) 
 
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

(defn ship-step [ship-ref]
	(with-connection *universe-db* (transaction
		(doseq [res (dosync (let [ship @ship-ref
					  resources (get @*resources-atom* [(:x ship) (:y ship)])]
					(if (not (nil? resources))
						(map (fn [res] [(:owner ship) (:item res) (:yield res)]) resources)
						[])))]
			(apply update-possessions res)
			(insert-rows "possessions" (conj res (. System currentTimeMillis))))))
	(get-possessions (:owner @ship-ref)))

(with-connection *universe-db* 
	(with-query-results results ["select owner, item, sum(qty) as qty, max(timestamp) as timestamp from possessions group by owner, item"]
		(doseq [r results]
			(let [user (keyword (:owner r))
			      type (keyword (:item r))
			      n (:qty r)]
				(update-possessions user type n)))))

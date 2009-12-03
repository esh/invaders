(ns universe
	(:use [clojure.contrib.sql]))

(. Class (forName "org.sqlite.JDBC"))

(def *universe-db* {:classname "org.sqlite.JDBC"
	   :subprotocol "sqlite"
	   :subname "../db/universe.db"})

(def *user-db* {:classname "org.sqlite.JDBC"
	:subprotocol "sqlite"
	:subname "../db/clients.db"})

(def *ship-meta*
	(let [ship-types (with-connection *universe-db* (with-query-results results ["select * from ship_types"]
				(apply merge (doall (map (fn [r] {(keyword (:name r)) r}) results)))))
	      ship-costs (with-connection *universe-db* (with-query-results results ["select * from ship_costs"]
				(apply merge-with into (doall (map (fn [r] {(keyword (:ship_type r)) [{:item (:item r) :qty (:qty r)}]}) results)))))]
		(merge-with (fn [type cost] (assoc type :cost cost)) ship-types ship-costs)))
		
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
		(atom (apply merge-with into (doall (map (fn [r] {[(:x r) (:y r)] [(assoc r :type "resources")]}) results)))))))
 
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

(defn get-ship-meta [] *ship-meta*)
	
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

(defn ship-at [x y] (first (filter (fn [s] (let [s @s] (and (= (:x s) x) (= (:y s) y)))) @*ships-atom*)))
											
(defn get-ship [id] (first (filter #(= (:id @%) id) @*ships-atom*)))

(defn move-ship [user from to]
	(let [ship (ship-at (nth from 0) (nth from 1))
	      target (ship-at (nth to 0) (nth to 1))]
		(if (and (not (nil? ship)) (= user (:owner @ship)) (nil? target))
			(let [id (:id @ship)
			      x (nth to 0)
			      y (nth to 1)]
				(dosync
					(ref-set ship (assoc @ship :x x :y y)))
				(with-connection *universe-db*
					(update-values :ships ["id=?" id] {:x x :y y}))))))

(defn find-dock [posx posy]
	(let [docks (for [x (range (- posx 1) (+ posx 2))
	     		  y (range (- posy 1) (+ posy 2))
			  :when (and (not (= x posx)) (not (= y posy)) (>= x 0) (>= y 0))] [x y])]
		(first (filter #(nil? (apply ship-at %)) docks))))

(defn create-ship [user id type]
	(let [res (dosync
		(let [ark (get-ship id)
		      dock (find-dock (:x @ark) (:y @ark))
		      possessions ((keyword user) @*possessions-atom*)
		      cost (:cost ((keyword type) *ship-meta*))]
			(if
				(and
					(= (:type @ark) "ark ship")
					(= (:owner @ark) user)
					(not (nil? dock))
					(reduce `and (map (fn [r] (>= @((keyword (:item r)) possessions) (:qty r))) cost)))
				(let [id (+ (apply max (map #(:id @%) @*ships-atom*)) 1)
				      cost (map (fn [c] [user (:item c) (- (:qty c))]) cost)
				      ship (ref {:id id :x (nth dock 0) :y (nth dock 1) :owner user :ship_type type :type "ships" :shields 1.0})]
					(doseq [c cost] (apply update-possessions cost))
					(reset! *ships-atom* (conj @*ships-atom* ship))
					{:updates cost :ship (vals (dissoc @ship :type))})
				nil)))]
		(if (not (nil? res))
			(do (with-connection *universe-db*
				(doseq [update (:updates res)] (insert-rows "possessions" (conj update (. System currentTimeMillis))))
				(insert-rows "ships" (:ship res)))))))
			
(with-connection *universe-db* 
	(with-query-results results ["select owner, item, sum(qty) as qty, max(timestamp) as timestamp from possessions group by owner, item"]
		(doseq [r results]
			(let [user (keyword (:owner r))
			      type (keyword (:item r))
			      n (:qty r)]
				(update-possessions user type n)))))

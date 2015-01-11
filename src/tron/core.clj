(ns tron.core
  (:require [quil.core :as q]))

(def size "size of the square arena" 30)
(def scale 20)
(def sleep-length "time in ms between turns" 200)

(def arena
  (mapv vec (partition size
              (repeatedly (* size size) #(ref nil)))))

(defn blank-arena []
  (dosync
    (doseq [row arena r row]
      (ref-set r nil))))

(defn setup []
  (q/color-mode :hsb)
  (q/smooth)
  (q/frame-rate 10))

(defn draw []
  (q/background 0)
  (dosync 
    (doseq [x (range 0 size)
            y (range 0 size)]
      (when-let [hue @(get-in arena [x y])]
        (q/fill (q/color hue 255 255))
        (q/rect (* scale x) (* scale y) scale scale)))))

(q/defsketch tron
  :title "TRON"
  :setup setup
  :draw draw
  :size [(* scale size) (* scale size)])

(defn valid-pos? [[i j]]
  (and (< -1 i size) (< -1 j size)))

(def legal-moves #{[0 1] [1 0] [0 -1] [-1 0]})

(defn valid-move? [from to]
  (contains? legal-moves (map - to from)))

(def ^:private bots-gen (atom 0))

(defn stop! [] (swap! bots-gen inc))

(defn biker [arena strategy]
  (let [look (fn [pos] (if (valid-pos? pos)
                         @(get-in arena pos)
                         :wall))
        gen @bots-gen] 
    (fn self [{:keys [state hue] :as agt-state}]
	    (dosync
	      (let [state' (strategy look state)
              pos' (:pos state')
              moved (when (and (valid-move? (:pos state) pos')
                            (valid-pos? pos')
                            (nil? @(get-in arena pos')))
                      (ref-set (get-in arena  (:pos state')) hue))]
         (if (and (= gen @bots-gen) moved)
	        (do
	          (Thread/sleep sleep-length)
	          (send-off *agent* self)
	          (assoc agt-state :state state'))
	        (do 
	          (println "arghhh" hue)
	          (assoc agt-state :dead true))))))))

(defn spawn-biker [strategy]
  (send-off (agent {:state {:pos [(rand-int size)
                                  (rand-int size)]}
                    :hue (rand-int 255)})
    (biker arena strategy)))


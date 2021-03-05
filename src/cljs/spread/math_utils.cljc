(ns spread.math-utils)

(def sqrt  #?(:clj  #(Math/sqrt    %)
              :cljs #(js/Math.sqrt %)))

(def log  #?(:clj  #(Math/log    %)
             :cljs #(js/Math.log %)))

(def pow  #?(:clj  #(Math/pow    %1 %2)
             :cljs #(js/Math.pow %1 %2)))

(defn cuad-curve-length
  "Calculates the length of a cuadratic curve given three points using the algorithm in
  https://web.archive.org/web/20131204200129/http://www.malczak.info:80/blog/quadratic-bezier-curve-length/

  p0x p0y - Cuadratic curve starting point
  p1x p1y - Cuadratic curve focus point
  p2x p2y - Cuadratic curve stop point"
  
  [p0x p0y p1x p1y p2x p2y]
  
  (let [ax (+ (- p0x (* 2 p1x)) p2x)
        ay (+ (- p0y (* 2 p1y)) p2y)
        bx (- (* 2 p1x) (* 2 p0x))
        by (- (* 2 p1y) (* 2 p0y))
        A (* 4 (+ (* ax ax) (* ay ay)))
        B (* 4 (+ (* ax bx) (* ay by)))
        C (+ (* bx bx) (* by by))
        
        Sabc (* 2 (sqrt (+ A B C)))
        A_2 (sqrt A)
        A_32 (* 2 A A_2)
        C_2 (* 2 (sqrt C))
        BA (/ B A_2)]
    (/ (+ (* A_32 Sabc)
          (* A_2 B (- Sabc C_2))
          (* (- (* 4 C A) (* B B)) (log (/ (+ (* 2 A_2) BA Sabc)
                                           (+ BA C_2)))))
       (* 4 A_32))))

(defn cuad-curve-focuses
  
  "Calculates nice focuses (positive and negative) for a cuadratic curve given its starting and end points"
  
  [x1 y1 x2 y2]
  (let [line-m (/ (- y1 y2)
                  (- x1 x2))
        line (fn [x] (- (+ (* line-m x) y1) (* x1 line-m)))
        perp-m (/ (- x1 x2)
                  (- y1 y2)
                  -1)
        cx (+ x1 (/ (- x2 x1) 2))
        cy (line cx)
        center-perp (fn [x] (- (+ (* perp-m x) cy) (* cx perp-m)))
        ;; TODO: make kdist a function of the length instead of a constant
        kdist 20]
    {:f1 [(- cx kdist) (center-perp (- cx kdist))]
     :f2 [(+ cx kdist) (center-perp (+ cx kdist)) ]}))

(defn outscribing-rectangle [[center-x center-y] radius]
  {:x (- center-x radius)
   :y (- center-y radius)
   :w (* 2 radius)
   :h (* 2 radius)})

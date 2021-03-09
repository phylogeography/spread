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
        k 5 ;; we can increase/decrease k to make the arc (focus distance) bigger/smaller
        ;; This was solved with wolframalfa using the next formula where
        ;; f=fx,e=fy,m=perp-m,c=cx,d=cy
        ;; It will yield two solutions (one for each focus)
        ;; Solve[mx+d-mc-y=0 && (x-c)^2+(y-d)^2=k^2, {x,y}]
        fx-fn (fn [sol-fn]
                ;; calculates focus-x, which is the point x that belongs to the perpendicular line
                ;; that goes thru the center, and also is at distance k from cx,cy
                (/ (sol-fn (+ (* cx (pow perp-m 2)) cx) (sqrt (* (pow k 2) (+ (pow perp-m 2) 1))))
                   (+ (pow perp-m 2) 1)))
        f1x (fx-fn +)
        f2x (fx-fn -)
        f1y (center-perp f1x)
        f2y (center-perp f2x)]
    {:f1 [f1x f1y]
     :f2 [f2x f2y]}))

(defn outscribing-rectangle [[center-x center-y] radius]
  {:x (- center-x radius)
   :y (- center-y radius)
   :w (* 2 radius)
   :h (* 2 radius)})

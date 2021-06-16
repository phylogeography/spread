(ns shared.math-utils)

(def sqrt  #?(:clj  #(Math/sqrt    %)
              :cljs #(js/Math.sqrt %)))

(def log  #?(:clj  #(Math/log    %)
             :cljs #(js/Math.log %)))

(def pow  #?(:clj  #(Math/pow    %1 %2)
             :cljs #(js/Math.pow %1 %2)))

(defn quad-curve-length
  "Calculates the length of a quadratic curve given three points using the algorithm in
  https://web.archive.org/web/20131204200129/http://www.malczak.info:80/blog/quadratic-bezier-curve-length/

  p0x p0y - Quadratic curve starting point
  p1x p1y - Quadratic curve focus point
  p2x p2y - Quadratic curve stop point"
  
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

(defn quad-curve-focuses
  
  "Calculates nice focuses (positive and negative) for a quadratic curve given its starting and end points"
  
  [x1 y1 x2 y2 curvature]
  (let [line-m (/ (- y1 y2)
                  (- x1 x2))
        ;; the line that goes from [x1,y1] to [x2,y2]
        line (fn [x] (- (+ (* line-m x) y1) (* x1 line-m)))
        perp-m (/ (- x1 x2)
                  (- y1 y2)
                  -1)
        cx (+ x1 (/ (- x2 x1) 2))
        cy (line cx)
        ;; a line perp to `line` that pass thru its center
        center-perp (fn [x] (- (+ (* perp-m x) cy) (* cx perp-m)))
                
        length (sqrt (+ (pow (- x2 x1) 2) (pow (- y2 y1) 2)))
        
        ;; calculates focus x, a point that belongs to `center-perp` line and is at distance `k`
        ;; from `cx`,`cy`
        ;; `sol-fn` can be `+` or `-` to get the fx below and avobe the line
        fx-fn (fn [sol-fn]
                ;; This was solved with wolframalfa using the next formula where
                ;; f=fx,e=fy,m=perp-m,c=cx,d=cy
                ;; It will yield two solutions (one for each focus)
                ;; Solve[mx+d-mc-y=0 && (x-c)^2+(y-d)^2=k^2, {x,y}]
                
                ;; calculates focus-x, which is the point x that belongs to the perpendicular line
                ;; that goes thru the center, and also is at distance k from cx,cy
                (/ (sol-fn (+ (* cx (pow perp-m 2)) cx) (sqrt (* (pow curvature 2) (+ (pow perp-m 2) 1))))
                   (+ (pow perp-m 2) 1)))
        f1x (fx-fn +)
        f2x (fx-fn -)
        f1y (center-perp f1x)
        f2y (center-perp f2x)]
    {:f1 [f1x f1y]
     :f2 [f2x f2y]}))

(defn outscribing-rectangle
  "Calculates x,y,w,h of the rectangle outscribing a circle of
  center-x,center-y and radius."
  [[center-x center-y] radius]

  {:x (- center-x radius)
   :y (- center-y radius)
   :w (* 2 radius)
   :h (* 2 radius)})

(defn map-coord->proj-coord
  "Convert from:
      - map-coord: [lat,lon]  coordinates in map lat,long coords, -180 <= lon <= 180, -90 <= lat <= 90
   into
      - proj-coord:   [x,y]      coordinates in map projection coords, 0 <= x <= 360, 0 <= y <= 180
  "
  [[long lat]]
  
  [(+ long 180) 
   (+ (* -1 lat) 90)])

(defn screen-coord->proj-coord
  "Convert from:
       - screen-coord: [x,y]      coordinates in screen pixels, 0 <= x <= map-width, 0 <= y <= map-height
   into
       - proj-coord:   [x,y]      coordinates in map projection coords, 0 <= x <= 360, 0 <= y <= 180
   `translate`: current map translation
   `scale`: current map scale
   `proj-scale`: the scale between the screen area and the map projection.
  "
  [translate scale proj-scale [screen-x screen-y]]
  
  (let [[tx ty] translate]
    [(/ (- screen-x tx) (* proj-scale scale))
     (/ (- screen-y ty) (* proj-scale scale))]))

(defn calc-zoom-for-view-box
  "Calculates a scale and a translation to fit the rectangle
   defined by `x1`,`y1` `x2`,`y2` fully zoomed.
   All parameter coordinates are in map proj-coord.
   `proj-scale`: is the scale between the screen area and the map projection.
   Assumes working with a map projection of 360x180."
  [x1 y1 x2 y2 proj-scale]
  (let [map-proj-width  360
        map-proj-height 180
        scale-x (/ map-proj-width  (- x2 x1))
        scale-y (/ map-proj-height (- y2 y1))
        scale (min scale-x scale-y)
        tx    (* -1 scale proj-scale x1)
        ty    (* -1 scale proj-scale y1)]
    
    {:translate [tx ty]
     :scale     scale}))

(defn normalize-color-str [color-str]
  (let [[_ r g b] (re-find #"#(..)(..)(..)" color-str)
        norm (fn [hex-str] (/ (js/parseInt hex-str 16) 255))]
    [(norm r) (norm g) (norm b)]))

(defn to-hex [n]
  (let [s (.toString n 16)]
    (if (= 1 (count s))
      (str "0" s)
      s)))

(defn denormalize-color [[r g b]]  
  (str "#"
       (to-hex (int (* 255 r)))
       (to-hex (int (* 255 g)))
       (to-hex (int (* 255 b)))))

(defn calculate-color [start end perc]
  (let [[rs gs bs] (normalize-color-str start)
        [re ge be] (normalize-color-str end)
        color (denormalize-color [(+ (* perc rs) (* (- 1 perc) re))
                                  (+ (* perc gs) (* (- 1 perc) ge))
                                  (+ (* perc bs) (* (- 1 perc) be))])] 
    color))

(defn calc-perc [from to x]
  (/ (- x from) (- to from)))

(defn build-scaler
  "Builds a function that given a number in [orig-from...orig-to] range will
  yield a proportional number in the [dest-from...dest-to] range."
  [orig-from orig-to dest-from dest-to]
  (fn [x]
    (let [p (calc-perc orig-from orig-to x)]
      (+ (* p (- dest-to dest-from)) dest-from))))

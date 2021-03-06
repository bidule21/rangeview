(ns rangeview.frontend.draw
   (:require [monet.canvas :as canvas]
             [rangeview.frontend.html :as html]))

(def colour
  {:black  "#000000"
   :white  "#FFFFFF"
   :red    "#FF0000"
   :green  "#00FF00"
   :yellow "#FFFF00"})

(defn canvas-context
  "Get a canvas context from a particular canvas element."
  [id]
  (canvas/monet-canvas (html/element id) "2d"))

(defn circle
  "Draw a circle."
  [target-canvas scale x y diameter stroke-colour fill-colour]
  (let [height (.-height (:canvas target-canvas))
        width (.-width (:canvas target-canvas))
        x2 (+ (* x scale) (/ width 2))
        y2 (+ (* y scale) (/ height 2))
        radius (* (/ diameter 2) scale)]
    (->
      (:ctx target-canvas)
      (canvas/stroke-style (stroke-colour colour))
      (canvas/fill-style (fill-colour colour))
      (canvas/begin-path)
      (canvas/arc {:x x2
                   :y y2
                   :r radius
                   :start-angle 0
                   :end-angle canvas/two-pi
                   :counter-clockwise? true})
      (canvas/fill)
      (canvas/stroke))))

(defn sighter-triangle
  "Draw a yellow triangle in the upper right corner of the canvas."
  [target-canvas]
  (let [height (.-height (:canvas target-canvas))
        width (.-width (:canvas target-canvas))
        smallest-dim (min height width)
        triangle-size (/ smallest-dim 4)]
    (->
      (:ctx target-canvas)
      (canvas/stroke-style (:yellow colour))
      (canvas/fill-style (:yellow colour))
      (canvas/begin-path)
      (canvas/move-to (- width triangle-size) 0)
      (canvas/line-to width 0)
      (canvas/line-to width triangle-size)
      (canvas/fill)
      (canvas/stroke))))

(defn target
  "Draw a target."
  [target-canvas scale rings]
  (doseq [ring (sort-by first > rings)]
    (let [size (first ring)
          stroke (second ring)
          fill (nth ring 2)]
      (circle target-canvas scale 0 0 size stroke fill))))

(defn shot
  "Draw a single shot on a target"
  [target-canvas scale shot calibre colour]
  (circle target-canvas scale (:x shot) (:y shot) calibre :black colour))

(defn shots
  "Draw some shots."
  [target-canvas scale shots calibre]
  (if (> (count shots) 0)
    (do
      (when (:sighter (last shots))
        (sighter-triangle target-canvas))
      (doseq [s (butlast shots)]
        (shot target-canvas scale s calibre :green))
      (shot target-canvas scale (last shots) calibre :red))
    (sighter-triangle target-canvas)))

(defn pixels-per-mm
  "How many pixels should we use per mm of real target?

   We need to control the zoom such that all shots are visible, but as little
   target as possible is shown."
  [target-canvas rings calibre shots]
  (let [cal (/ calibre 2)
        ring-sizes (sort (map first rings))
        nine-ring (nth ring-sizes 2)
        h-shot (->> shots (map :x) (map Math/abs) (apply max) (* 2) (+ cal))
        v-shot (->> shots (map :y) (map Math/abs) (apply max) (* 2) (+ cal))
        h-ring (max (->> ring-sizes (filter #(> % h-shot)) (first)) nine-ring)
        v-ring (max (->> ring-sizes (filter #(> % v-shot)) (first)) nine-ring)
        height (.-height (:canvas target-canvas))
        width (.-width (:canvas target-canvas))]
    (if (> h-ring v-ring)
      (/ width h-ring)
      (/ height v-ring))))

(defn repaint
  "Repaint the target and shots."
  [target-id rings calibre shot-data]
  (let [target-canvas-id (str "target" target-id "-canvas")
        target-canvas (canvas-context target-canvas-id)
        scale (pixels-per-mm target-canvas rings calibre shot-data)]
    (target target-canvas scale rings)
    (shots target-canvas scale shot-data calibre)))

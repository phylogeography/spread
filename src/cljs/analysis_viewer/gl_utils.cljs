(ns analysis-viewer.gl-utils)

(defn load-shader [gl type source]
  (let [shader-type (case type
                      :vertex   (.-VERTEX_SHADER gl)
                      :fragment (.-FRAGMENT_SHADER gl))
        shader (.createShader gl shader-type)]
    (.shaderSource gl shader source)
    (.compileShader gl shader)
    (if-not (.getShaderParameter gl shader (.-COMPILE_STATUS gl))
      (do
        (js/console.error "An error ocurred compiling shader" (.getShaderInfoLog gl shader))
        (.deleteShader gl shader)
        nil)

      shader)))

(defn link-shader-program [gl vert-source frag-source]
  (let [vert-shader (load-shader gl :vertex vert-source)
        frag-shader (load-shader gl :fragment frag-source)
        shader-program (.createProgram gl)]
    (doto gl
      (.attachShader shader-program vert-shader)
      (.attachShader shader-program frag-shader)
      (.linkProgram  shader-program))

    (if-not (.getProgramParameter gl shader-program (.-LINK_STATUS gl))
      (do
        (js/console.error "Error linking shader program" (.getProgramInfoLog gl shader-program))
        nil)

      shader-program)))

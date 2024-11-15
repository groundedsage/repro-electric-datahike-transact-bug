(ns prod
  (:require
   #?(:clj [clojure.edn :as edn])
   #?(:clj [clojure.java.io :as io])
   #?(:clj [clojure.tools.logging :as log])
   [contrib.assert :refer [check]]
   electric-starter-app.main
   #?(:clj [electric-starter-app.server-jetty :as jetty])
   [hyperfiddle.electric3 :as e]
   #?(:clj [hyperfiddle.entrypoint])
   #?(:cljs [hyperfiddle.electric-client3]))
  #?(:cljs (:require-macros [prod :refer [compile-time-resource]])))

(defmacro compile-time-resource [filename] (some-> filename io/resource slurp edn/read-string))

(def config
  (merge
    ;; Client and server versions must match in prod (dev is not concerned)
    ;; `src-build/build.clj` will compute the common version and store it in `resources/electric-manifest.edn`
    ;; On prod boot, `electric-manifest.edn`'s content is injected here.
    ;; Server is therefore aware of the program version.
    ;; The client's version is injected in the compiled .js file.
    (doto (compile-time-resource "electric-manifest.edn") prn)
    {:host "0.0.0.0", :port 8080,
     :resources-path "public/electric_starter_app"
     ;; shadow build manifest path, to get the fingerprinted main.sha1.js file to ensure cache invalidation
     :manifest-path "public/electric_starter_app/js/manifest.edn"}))

;;; Prod server entrypoint

#?(:clj
   (defn -main [& {:strs [] :as args}] ; clojure.main entrypoint, args are strings
     (log/info (pr-str config))
     (check string? (:hyperfiddle.electric/user-version config))
     (jetty/start-server!
       (fn [ring-req] (hyperfiddle.entrypoint/boot-server {} electric-starter-app.main/Main (e/server ring-req)))
       config)))

;;; Prod client entrypoint

#?(:cljs
   (defn ^:export start! []
     ((e/boot-client {} electric-starter-app.main/Main (e/server nil))
      #(js/console.log "Reactor success:" %)
      #(js/console.error "Reactor failure:" %))))

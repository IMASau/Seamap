(ns imas-seamap.reload
  "This code-base uses shadow-cljs modules to allow multiple entrypoints
  within a single codebase. However, if each entrypoint module has
  a :dev/after-load function they will *all* be called every time
  there is a reload, causing errors. Thus, the active module should
  register its `mount' function, where this namespace's single
  re-render function will be called.")

(defonce remount-fn (atom nil))

(defn ^:dev/after-load re-render
  "The `:dev/after-load` metadata causes this function to be called
  after shadow-cljs hot-reloads code. This function is called
  implicitly by its annotation."
  []
  (when-let [remount @remount-fn] (remount)))

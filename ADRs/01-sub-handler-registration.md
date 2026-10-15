# 01. Subscription Handler Registration

## Overview

Our current setup has every deployment core ns contain a map of maps, where the map behind the `:sub` key itself maps symbols to handlers: that is we iterate over the map, and call `(reg-sub k v)`.

We now want to change this for performance reasons, so we use more "layer 3" subscriptions (calculations, that depend on simpler layer-2 subscriptions which are simple db access).

## Proposal

We keep the map, but if the value is a map itself we handle that as a layer-4 sub:
```clojure
{:subs {::my-sub {:inputs [[:inp-a] [:inp-b]]
                  :handler subs/my-handler}}
}

; becomes

(reg-sub
  ::my-sub
  (fn [_]
    [(subscribe [:inp-a])
     (subscribe [:inp-b])])
  ; assumed signature of (fn [[a b] _] ...):
  subs/my-handler)
```

## Discussion and tradeoffs

The map is going to get much larger, because we will also be registering all the little layer-2 subs that have previously been done by direct db access.  We can partially mitigate this by having a common "core" config map, and then each separate deployment can merge on this core.

We can retain the old syntax, by extending the `register-handlers!` function.  It already dispatches on type of the val arg; now as well as function and vector it can also check for a map argument.

This should be backwards-compatible, and also allow us to move towards a configuration-driven approach (rather than hardcoding different core modules for each deployment)

## References

https://day8.github.io/re-frame/subscriptions/

# tmpreframe

A [re-frame](https://github.com/Day8/re-frame) application designed to ... well, that part is up to you.

## Development Mode

### Run application:

```
lein clean
lein repl
```
then evaluate
```
(load-file "scripts/figwheel.clj")
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3451](http://localhost:3451).

## Production Build


To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```

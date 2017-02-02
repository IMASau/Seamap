# IMAS Seamap

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

## Bundling third-party javascript

Due to availability, versioning, and other issues with using cljsjs
components, we supply our own copies or React, Leaflet, and others.
These are handled by
[create-react-app](https://github.com/facebookincubator/create-react-app),
in the `bundler` directory.  To add another library or update a
version, edit the dependencies in `packages.json`, make sure if
necessary it is imported and added to the `window` variable in
`index.js`, and create a new bundle by running `npm run build`.  It
will be called `main.<hash>.js` in the `build` directory; copy this on
top of our `bundled.js` (no, this isn't automated or hooked into our
build process at all).

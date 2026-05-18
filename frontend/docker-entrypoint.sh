#!/bin/sh
set -e

# Install dependencies on first run (volume-mounted node_modules is empty).
if [ ! -d /app/node_modules ] || [ -z "$(ls -A /app/node_modules 2>/dev/null)" ]; then
    echo "[entrypoint] node_modules empty — running yarn install"
    yarn install --frozen-lockfile
fi

# shadow-cljs requires the transpiled src/gen/* output to be present (and
# kept in sync) for the cljs build to resolve its JS deps. Kick off the babel
# watcher in the background, then run whatever the CMD asks for in the
# foreground (typically `yarn watch` for shadow-cljs). The babel watcher
# exits with the container.
if [ "$1" = "yarn" ] && [ "$2" = "run" ] && [ "$3" = "watch" ]; then
    echo "[entrypoint] starting babel watcher in background"
    yarn run build-js
    yarn run watch-js &

    # shadow-cljs only serves files under resources/public; the two CSS bundles
    # below are build artifacts that aren't checked in. Without them the app
    # renders unstyled. No watch variant exists for these, so rebuild on every
    # container start — edits to src/index.scss / src/ui/blueprint.scss need a
    # `yarn run build-css` (or container restart) to take effect.
    echo "[entrypoint] building css bundles"
    yarn run build-blueprint-css
    yarn run build-css
fi

exec "$@"

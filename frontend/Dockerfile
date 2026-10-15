# Seamap ClojureScript frontend — development image.
#
# `yarn watch` runs shadow-cljs, which needs BOTH a JVM (to compile cljs) and
# Node (for the JS toolchain). This image supplies both plus the npm deps, so a
# new machine doesn't need Java, Node, or Yarn installed locally.
#
# node_modules is baked into the image and surfaced at runtime via a named
# volume (see docker-compose.yml) so the bind-mounted source doesn't shadow it.
FROM node:24-bookworm

# JVM for shadow-cljs.
RUN apt-get update && apt-get install -y --no-install-recommends \
        default-jre-headless \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# package.json has several `link:./custom_modules/...` dependencies, so those
# directories must exist before `yarn install` runs.
COPY package.json yarn.lock ./
COPY custom_modules ./custom_modules

RUN yarn install --frozen-lockfile

# UTF-8 locale so the JVM (sun.jnu.encoding) can resolve asset filenames with
# non-ASCII characters; without it, static files like "… – ….png" 404.
ENV LANG=C.UTF-8

EXPOSE 3451 8777

CMD ["yarn", "watch"]

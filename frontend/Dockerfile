# node-sass 7.x (used by build-blueprint-css) only ships prebuilt binaries up
# to Node 16, so we pin Node 16 here to keep `npm install` clean. shadow-cljs
# itself needs a JVM; we add OpenJDK 17.
FROM node:16-bookworm-slim

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
        openjdk-17-jre-headless \
        ca-certificates \
        git \
        python3 \
        make \
        g++ \
    && rm -rf /var/lib/apt/lists/*
# yarn 1.x ships pre-installed in the node:16 image; nothing extra to do here.

WORKDIR /app

COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

EXPOSE 3451 8777 9630

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
CMD ["yarn", "run", "watch"]

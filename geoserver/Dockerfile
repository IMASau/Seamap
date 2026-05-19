# syntax=docker/dockerfile:1.6
#
# Mirrors the production deploy (see imas-data-infrastructure/roles/geoserver):
# Tomcat 9 + JRE 17, with the IMAS-built GeoServer WAR dropped into webapps/
# and GEOSERVER_DATA_DIR pointed at a bind-mounted clone of the
# geoserver-config-imas repo.
#
# The WAR itself is NOT baked into this image — the geoserver-fetch one-shot
# service downloads it into a named volume so version bumps don't require a
# rebuild. The entrypoint copies it into webapps/ at container start.

FROM tomcat:9-jre17-temurin

# Fonts (used by WMS label rendering) and updated CA bundle. GeoServer ships
# pure-Java readers for shapefile / GeoTIFF / etc.; native GDAL would only
# be needed for the GDAL extension, which IMAS prod doesn't install.
RUN apt-get update && apt-get install -y --no-install-recommends \
        ca-certificates \
        fonts-dejavu \
    && rm -rf /var/lib/apt/lists/*

# Strip the default Tomcat webapps; we only serve geoserver here.
RUN rm -rf /usr/local/tomcat/webapps/* /usr/local/tomcat/webapps.dist

COPY entrypoint.sh /srv/entrypoint.sh
RUN chmod +x /srv/entrypoint.sh

ENTRYPOINT ["/srv/entrypoint.sh"]
CMD ["catalina.sh", "run"]

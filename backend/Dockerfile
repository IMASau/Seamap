# Seamap Django backend — development image.
#
# Bundles the awkward native dependencies so a new machine only needs Docker:
#   - GDAL / ogr2ogr        (OGR2OGR_PATH in settings; shapefile subsetting)
#   - GEOS / PROJ           (shapely, pyproj, cartopy, geopandas, geoplot)
#   - unixODBC + msodbcsql18 (pyodbc / mssql-django talks to SQL Server)
#
# The source tree is bind-mounted at runtime (see docker-compose.yml), so this
# image only needs the Python dependencies baked in. `runserver` then picks up
# live code edits.
FROM python:3.11-slim-bookworm

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

# System libraries. build-essential + the -dev headers cover any package that
# falls back to building from source; gdal-bin provides the ogr2ogr binary.
RUN apt-get update && apt-get install -y --no-install-recommends \
        build-essential \
        ca-certificates \
        curl \
        gnupg \
        gdal-bin \
        libgdal-dev \
        libgeos-dev \
        libproj-dev \
        proj-bin \
        proj-data \
        unixodbc-dev \
    && rm -rf /var/lib/apt/lists/*

# Microsoft ODBC Driver 18 for SQL Server (the driver name referenced by
# local_settings.py). Pulled from Microsoft's Debian 12 apt repo.
RUN curl -fsSL https://packages.microsoft.com/keys/microsoft.asc \
        | gpg --dearmor -o /usr/share/keyrings/microsoft-prod.gpg \
    && curl -fsSL https://packages.microsoft.com/config/debian/12/prod.list \
        -o /etc/apt/sources.list.d/mssql-release.list \
    && apt-get update \
    && ACCEPT_EULA=Y apt-get install -y --no-install-recommends msodbcsql18 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# requirements.txt does `-r base_requirements.txt`, so both must be present.
COPY requirements.txt base_requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

EXPOSE 8000

# manage.py defaults DJANGO_SETTINGS_MODULE to webapp.local_settings.
CMD ["python", "manage.py", "runserver", "0.0.0.0:8000"]

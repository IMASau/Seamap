# syntax=docker/dockerfile:1.6
FROM python:3.11-slim-bookworm

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    DEBIAN_FRONTEND=noninteractive

# System dependencies: build tools, geo libraries (GDAL/GEOS/PROJ for
# geopandas, cartopy, shapely, pyproj), ODBC for mssql-django, plus curl/gnupg
# so we can add the Microsoft repository for msodbcsql18.
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
        unixodbc \
        unixodbc-dev \
    && rm -rf /var/lib/apt/lists/*

# Microsoft ODBC Driver 18 (Debian 12 / bookworm).
RUN curl -fsSL https://packages.microsoft.com/keys/microsoft.asc \
        | gpg --dearmor -o /usr/share/keyrings/microsoft-prod.gpg \
    && echo "deb [signed-by=/usr/share/keyrings/microsoft-prod.gpg arch=amd64,arm64,armhf] https://packages.microsoft.com/debian/12/prod bookworm main" \
        > /etc/apt/sources.list.d/mssql-release.list \
    && apt-get update \
    && ACCEPT_EULA=Y apt-get install -y --no-install-recommends \
        msodbcsql18 \
        mssql-tools18 \
        unzip \
        libicu72 \
    && ln -s /opt/mssql-tools18/bin/sqlcmd /usr/local/bin/sqlcmd \
    && ln -s /opt/mssql-tools18/bin/bcp    /usr/local/bin/bcp \
    && rm -rf /var/lib/apt/lists/*

# sqlpackage — Microsoft's DAC framework CLI. Used by scripts/capture-schema.sh
# to extract the live UTAS Seamap dev DB → .dacpac → schema.sql. Not used at
# container runtime; dbinit.py only invokes sqlcmd. Standalone Linux-x64
# build; the backend service is therefore pinned to linux/amd64 in
# docker-compose.yml. aka.ms/sqlpackage-linux redirects to the current
# release — the .dacpac format is forward-compatible so always-latest is safe.
RUN curl -fsSL "https://aka.ms/sqlpackage-linux" -o /tmp/sqlpackage.zip \
    && mkdir -p /opt/sqlpackage \
    && unzip -q /tmp/sqlpackage.zip -d /opt/sqlpackage \
    && chmod +x /opt/sqlpackage/sqlpackage \
    && ln -s /opt/sqlpackage/sqlpackage /usr/local/bin/sqlpackage \
    && rm /tmp/sqlpackage.zip

WORKDIR /app

COPY requirements.txt base_requirements.txt /app/
RUN pip install --upgrade pip \
    && pip install -r requirements.txt

COPY . /app/

EXPOSE 8000

CMD ["python", "manage.py", "runserver", "0.0.0.0:8000"]

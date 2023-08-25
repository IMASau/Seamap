from collections import namedtuple
import logging
import re

from django.conf import settings
from django.core.management.base import BaseCommand
from django.db import connections
from requests.adapters import HTTPAdapter, Retry
from shapely.geometry import shape
import pyodbc
import requests
import traceback

from catalogue.models import Layer


LayerFeature = namedtuple('LayerFeature', 'layer_id geom')

# Inserts a layer feature row; geom is added in binary MS-SSCLRT format
SQL_INSERT_LAYER_FEATURE = "INSERT INTO layer_feature_temp ( layer_id, geom ) VALUES ( ?, ? );"

# Inserts a layer feature log into the layer feature log table
SQL_LAYER_FEATURE_LOG = "INSERT INTO layer_feature_log ( layer_id, error, traceback ) VALUES ( %s, %s, %s );"


def http_session():
    retry_strategy = Retry(
        total=3,
        status_forcelist=[ 500, 502, 503, 504 ]
    )
    adapter = HTTPAdapter(max_retries=retry_strategy)
    http = requests.Session()
    http.mount("https://", adapter)
    http.mount("http://", adapter)
    return http


def mapserver_layer_query_url(layer):
    """
    For a given mapserver layer, find out from its server URL what the URL is that
    you can query feature info from.

    Parameters
    ----------
    layer : Layer
        layer to find mapserver query URL for.
    
    Returns
    -------
    string
        mapserver layer feature query URL
    """
    match = re.search(r'^(.+?)/services/(.+?)/MapServer/.+$', layer.server_url)
    map_server_url = f'{match.group(1)}/rest/services/{match.group(2)}/MapServer'

    try:
        r = requests.get(url=f"{map_server_url}/layers", params={'dynamicLayers': '1=1', 'f': 'json'})
    except Exception as e:
        raise Exception(f"Cannot retrieve data from mapserver ({map_server_url})") from e
    
    try:
        data = r.json()
    except Exception as e:
        raise Exception(f"Cannot decode mapserver response into JSON:\n{r.text}") from e

    try:
        server_layers = data['layers']
        server_layer = (
            server_layers
            if len(server_layers) == 1
            else list(
                filter(
                    lambda x: re.sub('[^a-zA-Z0-9]', '_', x['name']) == layer.layer_name[:-5], # so "Fishing Block [DPIPWE]" will match with "Fishing_Block__DPIPWE_62889"
                    server_layers
                )
            )
        )[0]  # get first layer if one layer, else filter the list
        assert server_layer
    except Exception as e:
        raise Exception(f"No server layer found in the mapserver data JSON:\n{data}") from e
    
    return f"{map_server_url}/{server_layer['id']}/query"


def get_geoserver_geojson(layer, server_url, result_offset=0):
    params = {
        'request':      'GetFeature',
        'service':      'WFS',
        'version':      '2.0.0',
        'typeNames':    layer.layer_name,
        'outputFormat': 'application/json',
        # Technically this is an optional feature, but we are 100%
        # geoserver for now so should be safe:
        'count':        100,
        'startIndex':   result_offset,
    }

    try:
        r = http_session().get(url=server_url, params=params)
    except Exception as e:
        raise Exception(f"Cannot retrieve GeoJSON from geoserver ({server_url})") from e

    try:
        data = r.json()
    except Exception as e:
        raise Exception(f"Cannot decode geoserver response into JSON:\n{r.text}") from e

    try:
        assert not data.get('error')
    except AssertionError as e:
        raise Exception(f"GeoJSON contains an error:\n{data}") from e
    
    try:
        assert data.get('features')
    except AssertionError as e:
        raise Exception(f"No features found in the GeoJSON:\n{data}") from e

    # We will use the presence of a 'next' link (not that
    # we use the link itself) to decide whether there is
    # more data to come or not. I think we may end up
    # making an extra call, for no data, but in the scheme
    # of things that's no great drama.
    links = data.get('links', [])
    has_next = any(link.get('rel') == 'next' for link in links)
    return data, has_next


def get_mapserver_geojson(server_url, result_offset=0):
    params = {
        'where':        '1=1',
        'f':            'geojson',
        'resultOffset': result_offset
    }

    try:
        r = requests.get(url=server_url, params=params)
    except Exception as e:
        raise Exception(f"Cannot retrieve GeoJSON from mapserver ({server_url})") from e

    try:
        data = r.json()
    except Exception as e:
        raise Exception(f"Cannot decode mapserver response into JSON:\n{r.text}") from e

    try:
        assert not data.get('error')
    except AssertionError as e:
        raise Exception(f"GeoJSON contains an error:\n{data}") from e

    try:
        assert data.get('features')
    except AssertionError as e:
        raise Exception(f"No features found in the GeoJSON:\n{data}") from e

    return data, data.get('exceededTransferLimit', False)


def get_features(layer, server_url, result_offset=0):
    if re.search(r'^(.+?)/services/(.+?)/MapServer/.+$', server_url):
        geojson, exceeded_transfer_limit = get_mapserver_geojson(server_url, result_offset)
    else:
        geojson, exceeded_transfer_limit = get_geoserver_geojson(layer, server_url, result_offset)

    features = [
        LayerFeature(
            layer.id,
            shape(feature['geometry']).wkt
        )
        for feature in geojson['features']
        if feature['geometry']
    ]

    return features, exceeded_transfer_limit


def insert_features(features, conn):
    try:
        with conn.cursor() as cursor:
            cursor.fast_executemany = True
            cursor.setinputsizes([None, (pyodbc.SQL_WVARCHAR, 0, 0)])
            cursor.executemany(SQL_INSERT_LAYER_FEATURE, features)
    except Exception as e:
        raise Exception("Unable to insert features") from e


def process_layer(layer):
    server_url = layer.server_url
    if re.search(r'^(.+?)/services/(.+?)/MapServer/.+$', server_url):
        server_url = mapserver_layer_query_url(layer)

    result_offset = 0
    conn = None
    try:
        conn = pyodbc.connect(
            "Driver={" +
            settings.DATABASES['default']['OPTIONS']['driver'] + "};"
            "Server=" + settings.DATABASES['default']['HOST'] + ";"
            "Database=" + settings.DATABASES['default']['NAME'] + ";"
            "UID=" + settings.DATABASES['default']['USER'] + ";"
            "PWD=" + settings.DATABASES['default']['PASSWORD'] + ";"
            "Trusted_Connection=no;"
        )

        exceeded_transfer_limit = True
        while exceeded_transfer_limit:
            logging.info(
                f"Retrieving {layer} ({layer.id}) features at offset {result_offset}..."
                if result_offset > 0 else
                f"Retrieving {layer} ({layer.id}) features..."
            )
            features, exceeded_transfer_limit = get_features(layer, server_url, result_offset)

            logging.info(f"Adding {len(features)} features to spatial index...")
            insert_features(features, conn)

            result_offset += len(features)
    except Exception as e:
        raise e
    finally:
        if conn:
            conn.close()


class Command(BaseCommand):
    def add_arguments(self, parser):
        parser.add_argument(
            '--layer_id',
            help="Specify a single layer ID you'd like to add to the feature index"
        )

    def handle(self, *args, **options):
        layer_id = int(options['layer_id']) if options['layer_id'] else None

        try:
            assert layer_id
        except AssertionError as e:
            logging.error(f"No layer_id argument was specified:\n{options}")
        else:
            try:
                layer = Layer.objects.get(id=layer_id)
            except AssertionError as e:
                logging.error(f"No layer found matching the specified layer_id: {layer_id}")
            else:
                try:
                    process_layer(layer)
                except Exception as e:
                    logging.error(f"Error processing layer {layer_id}", exc_info=e)
                    exception_traceback = ''.join(traceback.format_exception(type(e), e, e.__traceback__))
                    with connections['transects'].cursor() as cursor:
                        cursor.execute(SQL_LAYER_FEATURE_LOG, [layer_id, str(e), exception_traceback])
                else:
                    logging.info(f"Successfully processed layer {layer_id}")
                    with connections['transects'].cursor() as cursor:
                        cursor.execute(SQL_LAYER_FEATURE_LOG, [layer_id, None, None])

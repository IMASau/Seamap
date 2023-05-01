import requests
import re
import logging
import pyodbc
from django.core.management.base import BaseCommand
from django.conf import settings
from collections import namedtuple
from shapely.geometry import shape

from catalogue.models import Layer

LayerFeature = namedtuple('LayerFeature', 'layer_id geom')

# Inserts a layer feature row; geom is added in binary MS-SSCLRT format
SQL_INSERT_LAYER_FEATURE = "INSERT INTO layer_feature ( layer_id, geom ) VALUES ( ?, ? );"


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
        logging.error('Error at %s', 'division', exc_info=e)
    else:
        try:
            data = r.json()
        except Exception as e:
            logging.error('Error at %s\nResponse text:\n%s', 'division', r.text, exc_info=e)
        else:
            try:
                server_layers = data['layers']
                server_layer = (
                    server_layers
                    if len(server_layers) == 1
                    else list(filter(lambda x: x['name'] == layer.layer_name, server_layers))
                )[0]  # get first layer if one layer, else filter the list
                assert server_layer
            except Exception as e:
                logging.error('Error at %s\nResponse:\n%s', 'division', data, exc_info=e)
            else:
                return f"{map_server_url}/{server_layer['id']}/query"
    return None


def get_geoserver_geojson(layer, server_url, result_offset=0):
    params = {
        'request':      'GetFeature',
        'service':      'WFS',
        'version':      '2.0.0',
        'typeNames':    layer.layer_name,
        'outputFormat': 'application/json',
        # Technically this is an optional feature, but we are 100%
        # geoserver for now so should be safe:
        'count':        5000,
        'startIndex':   result_offset,
    }

    try:
        r = requests.get(url=server_url, params=params)
    except Exception as e:
        logging.error('Error at %s', 'division', exc_info=e)
    else:
        try:
            data = r.json()
        except Exception as e:
            logging.error('Error at %s\nResponse text:\n%s', 'division', r.text, exc_info=e)
        else:
            try:
                assert not data.get('error')
                assert data.get('features')
            except Exception as e:
                logging.error('Error at %s\nResponse:\n%s', 'division', data, exc_info=e)
            else:
                # We will use the presence of a 'next' link (not that
                # we use the link itself) to decide whether there is
                # more data to come or not. I think we may end up
                # making an extra call, for no data, but in the scheme
                # of things that's no great drama.
                links = data.get('links', [])
                has_next = any(link.get('rel') == 'next' for link in links)
                return data, has_next
    return None, False


def get_mapserver_geojson(server_url, result_offset=0):
    params = {
        'where':        '1=1',
        'f':            'geojson',
        'resultOffset': result_offset
    }

    try:
        r = requests.get(url=server_url, params=params)
    except Exception as e:
        logging.error('Error at %s', 'division', exc_info=e)
    else:
        try:
            data = r.json()
        except Exception as e:
            logging.error('Error at %s\nResponse text:\n%s', 'division', r.text, exc_info=e)
        else:
            try:
                assert not data.get('error')
                assert data.get('features')
            except Exception as e:
                logging.error('Error at %s\nResponse:\n%s', 'division', data, exc_info=e)
            else:
                return ( data, data.get('exceededTransferLimit', False) )
    return ( None, False )


def get_features(layer, server_url, result_offset=0):
    geojson = None
    exceeded_transfer_limit = False
    features = None

    if re.search(r'^(.+?)/services/(.+?)/MapServer/.+$', server_url):
        geojson, exceeded_transfer_limit = get_mapserver_geojson(server_url, result_offset)
    else:
        geojson = get_geoserver_geojson(layer, server_url, result_offset)
    if geojson:
        features = [
            LayerFeature(
                layer.id,
                shape(feature['geometry']).wkt
            )
            for feature in geojson['features']
            if feature['geometry']
        ]
    return ( features, exceeded_transfer_limit )


def insert_features(features, conn):
    try:
        with conn.cursor() as cursor:
            cursor.fast_executemany = True
            cursor.setinputsizes([None, (pyodbc.SQL_WVARCHAR, 0, 0)])
            cursor.executemany(SQL_INSERT_LAYER_FEATURE, features)
    except Exception as e:
        logging.error('Error at %s', 'division', exc_info=e)


def process_layer(layer):
    server_url = layer.server_url
    if re.search(r'^(.+?)/services/(.+?)/MapServer/.+$', server_url):
        server_url = mapserver_layer_query_url(layer)

    if server_url:
        result_offset = 0
        exceeded_transfer_limit = True
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
            while exceeded_transfer_limit:
                if result_offset > 0:
                    logging.info(
                        f"Retrieving {layer} ({layer.id}) features at offset {result_offset}...")
                else:
                    logging.info(
                        f"Retrieving {layer} ({layer.id}) features...")
                features, exceeded_transfer_limit = get_features(layer, server_url, result_offset)
                if features:
                    result_offset += len(features)
                    logging.info(
                        f"Adding {len(features)} features to spatial index...")
                    insert_features(features, conn)
                    logging.info(f"SUCCESS")
                else:
                    logging.info(f"FAILURE")
        except Exception as e:
            logging.error('Error at %s', 'division', exc_info=e)
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
        if layer_id:
            process_layer(Layer.objects.get(id=layer_id))

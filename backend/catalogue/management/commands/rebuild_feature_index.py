from django.core.management.base import BaseCommand
from django.conf import settings
import requests
import re
import logging
from shapely.geometry import shape
from collections import namedtuple
import pyodbc
import gc

from catalogue.models import Layer

LayerFeature = namedtuple('LayerFeature', 'layer_id geom')

SQL_RESET_LAYER_FEATURES = """
ALTER INDEX layer_geom ON layer_feature DISABLE;
TRUNCATE TABLE layer_feature;
"""

SQL_DELETE_LAYER_FEATURE = """
ALTER INDEX layer_geom ON layer_feature DISABLE;
DELETE FROM layer_feature WHERE layer_id = ?;
"""

SQL_INSERT_LAYER_FEATURE = "INSERT INTO layer_feature ( layer_id, geom ) VALUES ( ?, ? );"

SQL_REENABLE_SPATIAL_INDEX = """
UPDATE layer_feature SET geom.STSrid = 4326;
UPDATE layer_feature SET geom = geom.MakeValid();
ALTER INDEX layer_geom ON layer_feature REBUILD;
"""

def get_geoserver_features(layer):
    params = {
        'request':      'GetFeature',
        'service':      'WFS',
        'version':      '1.0.0',
        'typeNames':    layer.layer_name,
        'outputFormat': 'application/json',
    }

    try:
        r = requests.get(url=layer.server_url, params=params)
    except Exception as e:
        logging.error('Error at %s', 'division', exc_info=e)
    else:
        try:
            data = r.json()
            del r
        except Exception as e:
            logging.error('Error at %s\nResponse text:\n%s', 'division', r.text, exc_info=e)
            del r
        else:
            try:
                assert not data.get('error')
                assert data.get('features')
            except Exception as e:
                logging.error('Error at %s\nResponse:\n%s', 'division', data, exc_info=e)
            else:
                return data['features']
    return None


def mapserver_layer_query_url(layer):
    match = re.search(r'^(.+?)/services/(.+?)/MapServer/.+$', layer.server_url)
    map_server_url = f'{match.group(1)}/rest/services/{match.group(2)}/MapServer'

    try:
        r = requests.get(url=f"{map_server_url}/layers", params={'dynamicLayers': '1=1', 'f': 'json'})
    except Exception as e:
        logging.error('Error at %s', 'division', exc_info=e)
    else:
        try:
            data = r.json()
            del r
        except Exception as e:
            logging.error('Error at %s\nResponse text:\n%s', 'division', r.text, exc_info=e)
            del r
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


def get_mapserver_features(layer):
    params = {
        'where': '1=1',
        'f':     'geojson'
    }
    url = mapserver_layer_query_url(layer)

    features = [] # start with features list, and add to it with each paginated response

    if url:
        # loop through paginated responses until last page reached
        while True:
            if features:
                params['resultOffset'] = len(features)
            try:
                r = requests.get(url=url, params=params)
            except Exception as e:
                logging.error('Error at %s', 'division', exc_info=e)
                return None
            else:
                try:
                    data = r.json()
                    del r
                except Exception as e:
                    logging.error('Error at %s\nResponse text:\n%s', 'division', r.text, exc_info=e)
                    del r
                    return None
                else:
                    try:
                        assert not data.get('error')
                        assert data.get('features')
                    except Exception as e:
                        logging.error('Error at %s\nResponse:\n%s', 'division', data, exc_info=e)
                        return None
                    else:
                        features += data['features']
                        if not data.get('exceededTransferLimit'):
                            return features


def get_geometries(layer):
    features = None
    geometries = None

    if re.search(r'^(.+?)/services/(.+?)/MapServer/.+$', layer.server_url):
        features = get_mapserver_features(layer)
    else:
        features = get_geoserver_features(layer)

    if features:
        geometries = [
            shape(feature['geometry'])
            for feature in features
            if feature['geometry'] is not None
        ]
    del features
    return geometries


def add_features(layer, conn):
    logging.info(f"Retrieving {layer} ({layer.id}) features...")
    geometries = get_geometries(layer)

    if geometries is not None:
        # convert geometries to LayerFeature tuples
        layer_features = [
            LayerFeature(layer.id, geometry.wkt)
            for geometry in geometries
        ]
        
        # add the new LayerFeatures
        try:
            with conn.cursor() as cursor:
                logging.info(f"Adding {len(layer_features)} features to spatial index...")
                cursor.fast_executemany = True
                cursor.setinputsizes([None, (pyodbc.SQL_WVARCHAR, 0, 0)])
                for i in range(0, len(layer_features), 100000):
                    if len(layer_features) > 100000:
                        logging.info(f"Adding {i}/{len(layer_features)}...")
                    cursor.executemany(SQL_INSERT_LAYER_FEATURE, layer_features[i:i+100000])
        except Exception as e:
            logging.error('Error at %s', 'division', exc_info=e)
            logging.info('FAILURE')
        else:
            logging.info('SUCCESS')
    else:
        logging.info('FAILURE')
    del geometries


def insert_features(features, conn):
    try:
        with conn.cursor() as cursor:
            cursor.fast_executemany = True
            cursor.setinputsizes([None, (pyodbc.SQL_WVARCHAR, 0, 0)])
            cursor.executemany(SQL_INSERT_LAYER_FEATURE, features)
    except Exception as e:
        logging.error('Error at %s', 'division', exc_info=e)


def get_geoserver_geojson(layer, server_url):
    params = {
        'request':      'GetFeature',
        'service':      'WFS',
        'version':      '1.0.0',
        'typeNames':    layer.layer_name,
        'outputFormat': 'application/json',
    }

    try:
        r = requests.get(url=server_url, params=params)
    except Exception as e:
        logging.error('Error at %s', 'division', exc_info=e)
    else:
        try:
            data = r.json()
            del r
        except Exception as e:
            logging.error('Error at %s\nResponse text:\n%s', 'division', r.text, exc_info=e)
            del r
        else:
            try:
                assert not data.get('error')
                assert data.get('features')
            except Exception as e:
                logging.error('Error at %s\nResponse:\n%s', 'division', data, exc_info=e)
            else:
                return data
    return None


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
            del r
        except Exception as e:
            logging.error('Error at %s\nResponse text:\n%s', 'division', r.text, exc_info=e)
            del r
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
        geojson = get_geoserver_geojson(layer, server_url)
    if geojson:
        features = [
            LayerFeature(
                layer.id,
                shape(feature['geometry']).wkt
            )
            for feature in geojson['features']
            if feature['geometry']
        ]
    del geojson
    return ( features, exceeded_transfer_limit )


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
                "Driver={" + settings.DATABASES['default']['OPTIONS']['driver'] + "};"
                "Server=" + settings.DATABASES['default']['HOST'] + ";"
                "Database=" + settings.DATABASES['default']['NAME'] + ";"
                "UID=" + settings.DATABASES['default']['USER'] + ";"
                "PWD=" + settings.DATABASES['default']['PASSWORD'] + ";"
                "Trusted_Connection=no;"
            )
            while exceeded_transfer_limit:
                if result_offset > 0:
                    logging.info(f"Retrieving {layer} ({layer.id}) features at offset {result_offset}...")
                else:
                    logging.info(f"Retrieving {layer} ({layer.id}) features...")
                features, exceeded_transfer_limit = get_features(layer, server_url, result_offset)
                if features:
                    result_offset += len(features)
                    logging.info(f"Adding {len(features)} features to spatial index...")
                    insert_features(features, conn)
                    logging.info(f"SUCCESS")
                else:
                    logging.info(f"FAILURE")
        except Exception as e:
            logging.error('Error at %s', 'division', exc_info=e)
        finally:
            if conn is not None:
                conn.close()
                conn = None
                gc.collect()


class Command(BaseCommand):
    def add_arguments(self, parser):
        parser.add_argument(
            '--layer_id',
            help='Specify a single layer ID you\'d like to add to the feature index (for testing only)'
        )

    def handle(self, *args, **options):
        layer_id = int(options['layer_id']) if options['layer_id'] != None else None

        conn = None
        try:
            conn = pyodbc.connect(
                "Driver={" + settings.DATABASES['default']['OPTIONS']['driver'] + "};"
                "Server=" + settings.DATABASES['default']['HOST'] + ";"
                "Database=" + settings.DATABASES['default']['NAME'] + ";"
                "UID=" + settings.DATABASES['default']['USER'] + ";"
                "PWD=" + settings.DATABASES['default']['PASSWORD'] + ";"
                "Trusted_Connection=no;"
            )
            with conn.cursor() as cursor:
                if layer_id:
                    cursor.execute(SQL_DELETE_LAYER_FEATURE, [layer_id])
                else:
                    cursor.execute(SQL_RESET_LAYER_FEATURES)
        except Exception as e:
            logging.error('Error at %s', 'division', exc_info=e)
        finally:
            if conn is not None:
                conn.close()
                conn = None
        
        try:
            conn = pyodbc.connect(
                "Driver={" + settings.DATABASES['default']['OPTIONS']['driver'] + "};"
                "Server=" + settings.DATABASES['default']['HOST'] + ";"
                "Database=" + settings.DATABASES['default']['NAME'] + ";"
                "UID=" + settings.DATABASES['default']['USER'] + ";"
                "PWD=" + settings.DATABASES['default']['PASSWORD'] + ";"
                "Trusted_Connection=no;"
            )
            with conn.cursor() as cursor:
                if layer_id:
                    process_layer(Layer.objects.get(id=layer_id))
                else:
                    for layer in Layer.objects.all():
                        process_layer(layer)
        except Exception as e:
            logging.error('Error at %s', 'division', exc_info=e)
        finally:
            if conn is not None:
                with conn.cursor() as cursor:
                    cursor.execute(SQL_REENABLE_SPATIAL_INDEX)
                conn.close()

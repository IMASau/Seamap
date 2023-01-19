from django.core.management.base import BaseCommand
from django.db import connections
import requests
import re
import logging
from shapely.geometry import shape
from collections import namedtuple

from catalogue.models import Layer

LayerFeature = namedtuple('LayerFeature', 'layer_id feature_id geom type')

SQL_DELETE_LAYER_FEATURES = "DELETE FROM layer_feature;"

SQL_INSERT_LAYER_FEATURE = "INSERT INTO layer_feature (layer_id, feature_id, geom, type ) VALUES (%s, %s, GEOMETRY::STGeomFromText(%s, 4326), %s);"


def geoserver_request(layer):
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
        return None
    else:
        return r


def mapserver_layer_query_url(layer):
    match = re.search(r'^(.+?)/services/(.+?)/MapServer/.+$', layer.server_url)
    map_server_url = f'{match.group(1)}/rest/services/{match.group(2)}/MapServer'

    try:
        r = requests.get(url=map_server_url, params={'f': 'json'})
    except Exception as e:
        logging.error('Error at %s', 'division', exc_info=e)
        return None
    else:
        try:
            data = r.json()
        except Exception as e:
            logging.error('Error at %s\nResponse text:\n%s',
                          'division', r.text, exc_info=e)
            return None
        else:
            server_layers = data['layers']
            server_layer = (server_layers if len(server_layers) == 1 else list(filter(
                lambda x: x['name'] == layer.layer_name, server_layers)))[0]  # get first layer if one layer, else filter the list
            return f"{map_server_url}/{server_layer['id']}/query"


def mapserver_request(layer):
    params = {
        'where': '1=1',
        'f':     'geojson'
    }
    url = mapserver_layer_query_url(layer)

    if url:
        try:
            # TODO: Deal with paginated responses
            r = requests.get(url=url, params=params)
        except Exception as e:
            logging.error('Error at %s', 'division', exc_info=e)
            return None
        else:
            return r


def add_feature_params(layer, feature):
    if not feature['geometry']:
        return None

    o = dict(coordinates=feature['geometry']
             ['coordinates'], type=feature['geometry']['type'])
    geom = shape(o)
    wkt = geom.wkt

    return LayerFeature(layer.id, feature.get('id') or 'NoId!', wkt, feature['geometry']['type'])


def add_features(layer, successes, failures):
    logging.info(f"{layer} ({layer.id})...")
    r = mapserver_request(layer) if re.search(
        r'^(.+?)/services/(.+?)/MapServer/.+$', layer.server_url) else geoserver_request(layer)

    try:
        data = r.json()
    except Exception as e:
        logging.error('Error at %s\nResponse text:\n%s',
                      'division', r.text, exc_info=e)
        failures.append(layer)
        logging.info(f"FAILURE: {r.url}\n{r.text}\n")
        return None
    else:
        try:
            assert data.get('error') == None
        except Exception as e:
            logging.error('Error at %s\nError in response:\n%s',
                          'division', data.get('error'), exc_info=e)
            failures.append(layer)
            logging.info(f"FAILURE: {r.url}\n{r.text}\n")
        else:
            params = []

            for feature in data['features']:
                layer_feature = add_feature_params(layer, feature)
                if layer_feature:
                    params.append(layer_feature)

            try:
                logging.info('feature_count: %s', len(params))
                geom_types = {}
                for layer_feature in params:
                    geom_types[layer_feature.type] = geom_types.get(layer_feature.type, 0) + 1
                logging.info('geom_types: %s', geom_types)
                with connections['default'].cursor() as cursor:
                    # TODO: Bulk inserts? https://learn.microsoft.com/en-us/sql/relational-databases/import-export/import-bulk-data-by-using-bulk-insert-or-openrowset-bulk-sql-server?view=sql-server-ver16
                    cursor.executemany(SQL_INSERT_LAYER_FEATURE, params)
            except Exception as e:
                logging.error('Error at %s', 'division', exc_info=e)
                failures.append(layer)
                logging.info(f"FAILURE: {r.url}\n")
            else:
                successes.append(layer)
                logging.info(f"SUCCESS: {len(data['features'])}\n")


class Command(BaseCommand):
    def handle(self, *args, **options):
        ids = [2, 5, 6, 15, 27, 28, 29, 30, 31, 32, 35, 38, 39, 41, 42,
               43, 75, 125, 130, 138, 140, 145, 150, 157, 163, 166, 168]
        successes = []
        failures = []

        with connections['default'].cursor() as cursor:
            cursor.execute(SQL_DELETE_LAYER_FEATURES)

        for layer in Layer.objects.all():
            if layer.id in ids:
                add_features(layer, successes, failures)

        successes = [layer.id for layer in successes]
        logging.info("total successes: %s: %s", len(successes), successes)
        failures = [layer.id for layer in failures]
        logging.info("total failures: %s: %s", len(failures), failures)

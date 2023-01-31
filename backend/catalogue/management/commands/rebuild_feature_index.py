from django.core.management.base import BaseCommand
from django.db import connections
import requests
import re
import logging
from shapely.geometry import shape
import geopandas
from collections import namedtuple
import csv

from catalogue.models import Layer

LayerFeature = namedtuple('LayerFeature', 'layer_id feature_id geom type')

SQL_DELETE_LAYER_FEATURES = "DELETE FROM layer_feature;"

SQL_INSERT_LAYER_FEATURE = "INSERT INTO layer_feature (layer_id, feature_id, geom, type ) VALUES (%s, %s, GEOMETRY::STGeomFromText(%s, 4326), %s);"

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
        except Exception as e:
            logging.error('Error at %s\nResponse text:\n%s', 'division', r.text, exc_info=e)
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
        r = requests.get(url=map_server_url, params={'f': 'json'})
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
                except Exception as e:
                    logging.error('Error at %s\nResponse text:\n%s', 'division', r.text, exc_info=e)
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


def get_layer_feature(layer, feature):
    if not feature['geometry']:
        return None

    o = dict(
        coordinates=feature['geometry']['coordinates'],
        type=feature['geometry']['type']
    )
    geom = shape(o)
    wkt = geom.wkt

    return LayerFeature(layer.id, feature.get('id') or 'NoId!', wkt, feature['geometry']['type'])


def add_features(layer, successes, failures, to_csv=False):
    logging.info(f"{layer} ({layer.id})...")
    features = None
    if re.search(r'^(.+?)/services/(.+?)/MapServer/.+$', layer.server_url):
        features = get_mapserver_features(layer)
    else:
        features = get_geoserver_features(layer)

    if features is not None:
        # convert geojson features to LayerFeature tuples
        layer_features = []
        for feature in features:
            layer_feature = get_layer_feature(layer, feature)
            if layer_feature:
                layer_features.append(layer_feature)
        
        # add the new LayerFeatures
        try:
            if to_csv:
                with open('layer_feature.csv', 'a', newline='') as csvfile:
                    fieldnames = LayerFeature._fields
                    writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
                    writer.writerows([x._asdict() for x in layer_features])
            else:
                with connections['default'].cursor() as cursor:
                    # TODO: Bulk inserts? https://learn.microsoft.com/en-us/sql/relational-databases/import-export/import-bulk-data-by-using-bulk-insert-or-openrowset-bulk-sql-server?view=sql-server-ver16
                    cursor.executemany(SQL_INSERT_LAYER_FEATURE, layer_features)
        except Exception as e:
            logging.error('Error at %s', 'division', exc_info=e)
            logging.info('FAILURE')
            failures.append(layer)
            with open('failures.txt', 'a') as failures:
                failures.write(f',{layer.id}')
        else:
            logging.info('SUCCESS')
            successes.append(layer)
            with open('successes.txt', 'a') as successes:
                successes.write(f',{layer.id}')
    else:
        logging.info('FAILURE')
        failures.append(layer)
        with open('failures.txt', 'a') as failures:
            failures.write(f',{layer.id}')


def geometry_distance(geometryA, geometryB):
    t1 = geopandas.GeoSeries(geometryA, crs=4326).to_crs(3857)
    t2 = geopandas.GeoSeries(geometryB, crs=4326).to_crs(3857)
    return t1.distance(t2).values[0]


def group_geometries(geometries, index, group, ungrouped):
    geometry = geometries[index]
    ungrouped.remove(index)
    group.add(index)
    for i, other in enumerate(geometries):
        if i in ungrouped and geometry_distance(geometry, other) < 400:
            group_geometries(geometries, i, group, ungrouped)    


def get_feature_groups(layer):
    logging.info(f"{layer} ({layer.id})...")
    features = None

    # get our features
    if re.search(r'^(.+?)/services/(.+?)/MapServer/.+$', layer.server_url):
        features = get_mapserver_features(layer)
    else:
        features = get_geoserver_features(layer)

    if features is not None:
        geometries = [
            shape(feature['geometry'])
            for feature in features
            if feature['geometry'] is not None
        ]

        # group the features
        groups = []
        ungrouped = set(range(len(geometries)))
        for i in range(len(geometries)):
            if i in ungrouped:
                group = set()
                group_geometries(geometries, i, group, ungrouped)
                groups.append(group)
        return groups
    return None

            


class Command(BaseCommand):
    def add_arguments(self, parser):
        parser.add_argument(
            '--to_csv',
            help='Set to true if you want output to go to a csv file instead of a database table'
        )

    def handle(self, *args, **options):
        to_csv = options['to_csv'].lower() in ['t', 'true'] if options['to_csv'] != None else False
        successes = []
        failures = []

        if to_csv:
            with open('layer_feature.csv', 'w', newline='') as csvfile:
                fieldnames = LayerFeature._fields
                writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
                writer.writeheader()
        else:
            with connections['default'].cursor() as cursor:
                cursor.execute(SQL_DELETE_LAYER_FEATURES)

        for layer in Layer.objects.all():
            if layer.id == 2:
                get_feature_groups(layer)

        successes = [layer.id for layer in successes]
        logging.info("total successes: %s: %s", len(successes), successes)
        failures = [layer.id for layer in failures]
        logging.info("total failures: %s: %s", len(failures), failures)

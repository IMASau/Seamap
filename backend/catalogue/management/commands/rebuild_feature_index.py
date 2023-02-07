from django.core.management.base import BaseCommand
import requests
import re
import logging
from shapely.geometry import shape, box
import geopandas
from collections import namedtuple
import pyodbc
from django.conf import settings

from catalogue.models import Layer

LayerFeature = namedtuple('LayerFeature', 'layer_id geom')

SQL_RESET_LAYER_FEATURES = """
ALTER INDEX layer_geom ON layer_feature DISABLE;
TRUNCATE TABLE layer_feature;
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


def get_geometries(layer):
    features = None

    if re.search(r'^(.+?)/services/(.+?)/MapServer/.+$', layer.server_url):
        features = get_mapserver_features(layer)
    else:
        features = get_geoserver_features(layer)

    if features:
        return [
            shape(feature['geometry'])
            for feature in features
            if feature['geometry'] is not None
        ]
    return None


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
                logging.info(f"Adding to spatial index...")
                cursor.fast_executemany = True
                cursor.setinputsizes([None, (pyodbc.SQL_WVARCHAR, 0, 0)])
                cursor.executemany(SQL_INSERT_LAYER_FEATURE, layer_features)
        except Exception as e:
            logging.error('Error at %s', 'division', exc_info=e)
            logging.info('FAILURE')
        else:
            logging.info('SUCCESS')
    else:
        logging.info('FAILURE')


def geometry_distance(geometryA, geometryB):
    # TODO: Efficiency could be improved by moving geoseries conversion outside the function.
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


def get_features_layer_coverage(layer):
    geometries = get_geometries(layer)
    if geometries:
        geoseries = geopandas.GeoSeries(geometries, crs=4326)
        feature_area = sum(v for v in geoseries.to_crs(crs=3857).area.values)
        bounds = box(*geoseries.total_bounds)
        bounds_area = geopandas.GeoSeries(bounds, crs=4326).to_crs(crs=3857).area.values[0]
        return feature_area / bounds_area


def get_feature_groups(layer):
    logging.info(f"{layer} ({layer.id})...")
    geometries = get_geometries(layer)

    if geometries:
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
    def handle(self, *args, **options):
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
                cursor.execute(SQL_RESET_LAYER_FEATURES)
            for layer in Layer.objects.all():
                add_features(layer, conn)
        except Exception as e:
            logging.error('Error at %s', 'division', exc_info=e)
        finally:
            if conn is not None:
                with conn.cursor() as cursor:
                    cursor.execute(SQL_REENABLE_SPATIAL_INDEX)
                conn.close()

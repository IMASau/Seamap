from django.core.management.base import BaseCommand
from django.core.files.storage import default_storage
from django.core.files.base import ContentFile
from django.db import connections
from collections import namedtuple
import logging
import json

# groups the list of data by network
def group_networks(data):
    networks = set(map(lambda v: v.get('NETWORK'), data))
    return [{'network': network, 'value': [v for v in data if v.get('NETWORK') == network]} for network in networks]


# groups the list of data by park
def group_parks(data):
    parks = set(map(lambda v: v.get('PARK'), data))
    return [{'park': park, 'value': [v for v in data if v.get('PARK') == park]} for park in parks]


# summarises the list of data by each year
def summarise_by_year(data):
    years = set(map(lambda v: v.get('YEAR'), data))
    return [{
        'year': year,
        'imagery_count': sum(v.get('imagery_count') for v in data if v.get('YEAR') == year),
        'video_count': sum(v.get('video_count') for v in data if v.get('YEAR') == year),
        'sediment_count': sum(v.get('sediment_count') for v in data if v.get('YEAR') == year),
        'bathymetry_count': sum(v.get('bathymetry_count') for v in data if v.get('YEAR') == year)
    } for year in years]


SQL_GET_NETWORK_STATS = """
SELECT 
  NETWORK,
  YEAR,
  imagery_count,
  video_count,
  sediment_count,
  bathymetry_count
FROM VW_TIMELINE_COUNT_STATS_NETWORKS
"""

SQL_GET_PARK_STATS = """
SELECT 
  NETWORK,
  PARK,
  YEAR,
  imagery_count,
  video_count,
  sediment_count,
  bathymetry_count
FROM VW_TIMELINE_COUNT_STATS_PARKS
"""

class Command(BaseCommand):
    def handle(self, *args, **options):
        with connections['default'].cursor() as cursor:
            # networks
            logging.info(f'Retrieving networks research effort statistics')
            cursor.execute(SQL_GET_NETWORK_STATS)

            columns = [col[0] for col in cursor.description]
            namedrow = namedtuple('Result', columns)
            results = [namedrow(*row) for row in cursor.fetchall()]

            networks_data = [row._asdict() for row in results]

            networks = group_networks(networks_data)
            networks_summary = [
                {**v, **{'value': summarise_by_year(v.get('value'))}}
                for v in networks
            ]

            for network in networks_summary:
                network_name = network['network']
                filepath = f'research_effort/{network_name}.json'
                logging.info(f'Saving habitat statistics for {network_name}')
                default_storage.delete(filepath)
                default_storage.save(filepath, ContentFile(json.dumps(network['value'])))

            # parks
            logging.info(f'Retrieving parks research effort statistics')
            cursor.execute(SQL_GET_PARK_STATS)

            columns = [col[0] for col in cursor.description]
            namedrow = namedtuple('Result', columns)
            results = [namedrow(*row) for row in cursor.fetchall()]

            parks_data = [row._asdict() for row in results]

            networks = group_networks(parks_data)
            parks = [{**v, **{'value': group_parks(v.get('value'))}} for v in networks]
            parks_summary = [
                {**v, **{'value': [
                    {**w, **{'value': summarise_by_year(w.get('value'))}}
                    for w in v.get('value')
                ]}} for v in parks
            ]

            for network in parks_summary:
                network_name = network['network']
                for park in network.get('value'):
                    park_name = park['park']
                    filepath = f'research_effort/{network_name}/{park_name}.json'
                    logging.info(f'Saving habitat statistics for {network_name} - {park_name}')
                    default_storage.delete(filepath)
                    default_storage.save(filepath, ContentFile(json.dumps(park['value'])))

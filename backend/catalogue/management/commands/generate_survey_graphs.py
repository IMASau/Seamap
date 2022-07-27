from django.core.management.base import BaseCommand
from django.core.files.storage import default_storage
from django.core.files import File
from django.db import connections
from collections import namedtuple
import matplotlib.pyplot as plt
from matplotlib.ticker import MaxNLocator
from io import BytesIO

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


def generate_graph(data, title, directory, exclude_old_data):
    filepath = '{directory}/{title}.png'.format(
        directory=directory,
        title=title
    )

    if exclude_old_data:
        title = title + ' (2000 onwards)'
        data = [v for v in data if v.get('year') >= 2000]

    fig = plt.figure()

    bar_width = 0.25
    plt.bar(
        [v.get('year') for v in data],
        [v.get('imagery_count') for v in data],
        color='#3C67BC', width=bar_width, label='Imagery (campaigns)'
    )
    plt.bar(
        [v.get('year') + bar_width for v in data],
        [v.get('video_count') for v in data],
        color='#EA722B', width=bar_width, label='Video (campaigns)'
    )
    plt.bar(
        [v.get('year') + 2 * bar_width for v in data],
        [v.get('sediment_count') for v in data],
        color='#9B9B9B', width=bar_width, label='Sediment (surveys)'
    )
    plt.bar(
        [v.get('year') + 3 * bar_width for v in data],
        [v.get('bathymetry_count') for v in data],
        color='#FFB800', width=bar_width, label='Bathymetry (surveys)'
    )

    plt.legend()
    plt.xlabel('Year')
    plt.ylabel('Survey Effort')
    plt.title(title)

    # only allow integer ticks
    ax = fig.gca()
    ax.xaxis.set_major_locator(MaxNLocator(integer=True))
    ax.yaxis.set_major_locator(MaxNLocator(integer=True))

    # saving
    bytes_io = BytesIO()

    fig.savefig(bytes_io, format='png')
    default_storage.delete(filepath)
    default_storage.save(filepath, File(bytes_io, ''))

    bytes_io.close()


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
            cursor.execute(SQL_GET_NETWORK_STATS)

            columns = [col[0] for col in cursor.description]
            namedrow = namedtuple('Result', columns)
            results = [namedrow(*row) for row in cursor.fetchall()]

            networks_data = [row._asdict() for row in results]

            networks = group_networks(networks_data)
            networks_summary = [
                v | {'value': summarise_by_year(v.get('value'))}
                for v in networks
            ]

            for network in networks_summary:
                title = network.get('network')
                generate_graph(network.get('value'), title, 'survey_graphs/networks/all_data', False)
                generate_graph(network.get('value'), title, 'survey_graphs/networks/post_2000', True)

            # parks
            cursor.execute(SQL_GET_PARK_STATS)

            columns = [col[0] for col in cursor.description]
            namedrow = namedtuple('Result', columns)
            results = [namedrow(*row) for row in cursor.fetchall()]

            parks_data = [row._asdict() for row in results]

            networks = group_networks(parks_data)
            parks = [v | {'value': group_parks(v.get('value'))} for v in networks]
            parks_summary = [
                v | {'value': [
                    w | {'value': summarise_by_year(w.get('value'))}
                    for w in v.get('value')
                ]} for v in parks
            ]

            for network in parks_summary:
                network_name = network.get('network')
                for park in network.get('value'):
                    park_name = park.get('park')
                    title = '{network} - {park}'.format(network=network_name, park=park_name)
                    generate_graph(park.get('value'), title, 'survey_graphs/parks/all_data', False)
                    generate_graph(park.get('value'), title, 'survey_graphs/parks/all_data', True)

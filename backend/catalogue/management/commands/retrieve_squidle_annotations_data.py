import requests
from requests.adapters import HTTPAdapter, Retry
from urllib.parse import parse_qs
from django.conf import settings
from django.core.management.base import BaseCommand
import logging
import json
from pyquery import PyQuery
from sqapi.api import SQAPI, query_filter as qf

from catalogue.models import AmpDepthZones, KeyedLayer, Layer, SquidleAnnotationsData

class Command(BaseCommand):
    network_boundary_layer: Layer
    park_boundary_layer: Layer
    http_session: requests.Session
    api: SQAPI
    additional_filters: list
    qsparams: dict
    template: str
    results_per_page: int

    def start_http_session(self) -> None:
        retry_strategy = Retry(
            total=3,
            status_forcelist=[ 500, 502, 503, 504 ]
        )
        adapter = HTTPAdapter(max_retries=retry_strategy)
        http_session = requests.Session()
        http_session.mount("https://", adapter)
        http_session.mount("http://", adapter)
        self.http_session = http_session


    def geojson_boundary(self, network: str, park: str = None) -> list:
        boundary_layer = self.park_boundary_layer if park else self.network_boundary_layer
        url = boundary_layer.server_url
        params = {
            'request':      'GetFeature',
            'service':      'WFS',
            'version':      '2.0.0',
            'typeNames':    boundary_layer.layer_name,
            'outputFormat': 'application/json',
            'cql_filter': (f"RESNAME='{park}'" if park else f"NETNAME='{network}'")
        }
        r = self.http_session.get(url, params=params)
        data = r.json()
        boundary_type = data['features'][0]['geometry']['type']
        if boundary_type == 'Polygon':
            return [data['features'][0]['geometry']['coordinates']]
        elif boundary_type == 'MultiPolygon':
            return data['features'][0]['geometry']['coordinates']
        else:
            raise Exception(f"Unexpected boundary type: {boundary_type}")


    def get_squidle_annotations(self, geojson: list, min: int, max: int, highlights: bool) -> str:
        r = self.api.get("/api/annotation/tally/label", qsparams=self.qsparams)
        r.template(self.template)
        r.results_per_page(self.results_per_page)

        # filters
        r.filter("point", "has", qf("media", "has", qf("poses", "any", qf("geom", "geo_in_mpolyh_xy", geojson))))
        if highlights:
            r.filter("point", "has", qf("media", "has", qf("annotations", "any", qf("annotations", "any", qf("tags", "any", qf("id", "eq", "348"))))))
        if min:
            r.filter("point", "has", qf("media", "has", qf("poses", "any", qf("dep", "gt", min))))
        if max:
            r.filter("point", "has", qf("media", "has", qf("poses", "any", qf("dep", "lte", max))))

        for additional_filter in self.additional_filters:
            r._filters.append(additional_filter)

        data = r.execute().text
        tree = PyQuery(data)
        if (tree("div.tally-chart-row") and tree("div.tally-chart-row").text()) or (tree("div.chart-container") and tree("div.chart-container").text()):
            return data
        else:
            return None


    def get_network_depth_zones(self) -> dict:
        """
        Retrieves and organizes AMP depth zones by network.

        This function queries the `AmpDepthZones` model to retrieve all distinct depth zones
        for each network, organizes them first by network and then by park, and returns a
        structured dictionary.

        Returns:
            dict: A dictionary where the keys are network names and the values are dictionaries
                of regions, which in turn contain lists of depth zones. The structure is as follows:
                {
                    'network_name': [
                        {
                            'zonename': str,
                            'min': int,
                            'max': int
                        },
                        ...
                    ],
                    ...
                }

        Example:
            >>> get_network_depth_zones()
            {
                'South-east': [
                        {'zonename': 'mesophotic', 'min': 30, 'max': 70},
                        ...
                    ],
                ...
            }
        """
        amp_depth_zones = AmpDepthZones.objects.all().values('netname', 'zonename', 'min', 'max').distinct()  # [{'netname': 'South-east', 'zonename': 'mesophotic', 'min': 30, 'max': 70}, ...]
        network_depth_zones = {} # {'South-east': [{'zonename': 'mesophotic', 'min': 30, 'max': 70}, ...], ...}
        for amp_depth_zone in amp_depth_zones:
            if amp_depth_zone['netname'] not in network_depth_zones:
                network_depth_zones[amp_depth_zone['netname']] = []
            network_depth_zones[amp_depth_zone['netname']].append({k: amp_depth_zone[k] for k in ['zonename', 'min', 'max']})
        return network_depth_zones


    def get_park_depth_zones(self) -> dict:
        """
        Retrieves and organizes AMP depth zones by network and park.

        This function queries the `AmpDepthZones` model to retrieve all distinct depth zones,
        organizes them first by network and then by park, and returns a structured dictionary.

        Returns:
            dict: A dictionary where the keys are network names and the values are dictionaries
                of regions, which in turn contain lists of depth zones. The structure is as follows:
                {
                    'network_name': {
                        'park_name': [
                            {
                                'zonename': str,
                                'min': int,
                                'max': int
                            },
                            ...
                        ],
                        ...
                    },
                    ...
                }

        Example:
            >>> get_park_depth_zones()
            {
                'South-east': {
                    'Beagle': [
                        {'zonename': 'mesophotic', 'min': 30, 'max': 70},
                        ...
                    ],
                    ...
                },
                ...
            }
        """
        amp_depth_zones = AmpDepthZones.objects.all().values('netname', 'resname', 'zonename', 'min', 'max').distinct() # [{'netname': 'South-east', 'resname': 'Beagle', 'zonename': 'mesophotic', 'min': 30, 'max': 70}, ...]
        network_depth_zones = {} # {'South-east': [{'resname': 'Beagle', 'zonename': 'mesophotic', 'min': 30, 'max': 70}, ...], ...}
        for amp_depth_zone in amp_depth_zones:
            if amp_depth_zone['netname'] not in network_depth_zones:
                network_depth_zones[amp_depth_zone['netname']] = []
            network_depth_zones[amp_depth_zone['netname']].append({k: amp_depth_zone[k] for k in ['resname', 'zonename', 'min', 'max']})
        park_depth_zones = {} # {'South-east': {'Beagle': [{'zonename': 'mesophotic', 'min': 30, 'max': 70}, ...], ...}, ...}
        for network, depth_zones in network_depth_zones.items():
            if network not in park_depth_zones:
                park_depth_zones[network] = {}
            for depth_zone in depth_zones:
                if depth_zone['resname'] not in park_depth_zones[network]:
                    park_depth_zones[network][depth_zone['resname']] = []
                park_depth_zones[network][depth_zone['resname']].append({k: depth_zone[k] for k in ['zonename', 'min', 'max']})
        return park_depth_zones


    def add_arguments(self, parser):
        parser.add_argument(
            '--skip_existing',
            help='If true, skips retrieving squidle annotations data where we already have it',
        )


    def handle(self, *args, **options):
        skip_existing = options['skip_existing'].lower() in ['t', 'true'] if options['skip_existing'] != None else False

        self.start_http_session()
        self.network_boundary_layer = KeyedLayer.objects.get(keyword='data-report-boundary-network-simplified').layer
        self.park_boundary_layer = KeyedLayer.objects.get(keyword='data-report-boundary-simplified').layer
        self.api = SQAPI(host="https://squidle.org", api_key=settings.SQUIDLE_API_KEY)

        wordpress_data = self.http_session.get(f"{settings.WORDPRESS_URL}wp-json/wp/v2/region_report?acf_format=standard").json()[0]
        self.additional_filters = json.loads(wordpress_data['region_report_squidle_annotations_filters']) if wordpress_data.get('region_report_squidle_annotations_filters') else []
        self.qsparams = {k: v[0] for k, v in parse_qs(wordpress_data['region_report_squidle_query_string_parameters']).items()} if wordpress_data.get('region_report_squidle_query_string_parameters') else {}
        self.template = self.qsparams.pop('template', 'models/annotation/tally_chart_translated.html')
        self.results_per_page = int(self.qsparams.pop('results_per_page', '300'))

        park_depth_zones = self.get_park_depth_zones()
        for network, park_depth_zone in park_depth_zones.items():
            for park, depth_zones in park_depth_zone.items():
                try:
                    logging.info(f"Retrieving GeoJSON for {network} > {park}")
                    geojson = self.geojson_boundary(network, park)
                except Exception as e:
                    logging.error(f"Error retrieving GeoJSON for {network} > {park}", exc_info=e)
                else:
                    for depth_zone in depth_zones:
                        for highlights in [True, False]:
                            if skip_existing and SquidleAnnotationsData.objects.filter(network=network, park=park, depth_zone=depth_zone['zonename'], highlights=highlights).exists():
                                logging.info(f"Skipping {network} > {park} > {depth_zone['zonename']} ({'Highlights' if highlights else 'No Highlights'})")
                            else:
                                try:
                                    logging.info(f"Processing {network} > {park} > {depth_zone['zonename']} ({'Highlights' if highlights else 'No Highlights'})")
                                    annotations_data = self.get_squidle_annotations(geojson, depth_zone['min'], depth_zone['max'], highlights)
                                    SquidleAnnotationsData.objects.update_or_create(
                                        network=network,
                                        park=park,
                                        depth_zone=depth_zone['zonename'],
                                        highlights=highlights,
                                        defaults={
                                            "annotations_data": annotations_data
                                        }
                                    )
                                except Exception as e:
                                    logging.error(f"Error processing {network} > {park} > {depth_zone['zonename']} ({'Highlights' if highlights else 'No Highlights'})", exc_info=e)
                    for highlights in [True, False]:
                        if skip_existing and SquidleAnnotationsData.objects.filter(network=network, park=park, depth_zone=None, highlights=highlights).exists():
                            logging.info(f"Skipping {network} > {park} > All Depths ({'Highlights' if highlights else 'No Highlights'})")
                        else:
                            try:
                                logging.info(f"Processing {network} > {park} > All Depths ({'Highlights' if highlights else 'No Highlights'})")
                                annotations_data = self.get_squidle_annotations(geojson, None, None, highlights)
                                SquidleAnnotationsData.objects.update_or_create(
                                    network=network,
                                    park=park,
                                    depth_zone=None,
                                    highlights=highlights,
                                    defaults={
                                        "annotations_data": annotations_data
                                    }
                                )
                            except Exception as e:
                                logging.error(f"Error processing {network} > {park} > All Depths ({'Highlights' if highlights else 'No Highlights'})", exc_info=e)

        network_depth_zones = self.get_network_depth_zones()
        for network, depth_zones in network_depth_zones.items():
            try:
                logging.info(f"Retrieving GeoJSON for {network}")
                geojson = self.geojson_boundary(network)
            except Exception as e:
                logging.error(f"Error retrieving GeoJSON for {network}", exc_info=e)
            else:
                for depth_zone in depth_zones:
                    for highlights in [True, False]:
                        if skip_existing and SquidleAnnotationsData.objects.filter(network=network, park=None, depth_zone=depth_zone['zonename'], highlights=highlights).exists():
                            logging.info(f"Skipping {network} > {depth_zone['zonename']} ({'Highlights' if highlights else 'No Highlights'})")
                        else:
                            try:
                                logging.info(f"Processing {network} > {depth_zone['zonename']} ({'Highlights' if highlights else 'No Highlights'})")
                                annotations_data = self.get_squidle_annotations(geojson, depth_zone['min'], depth_zone['max'], highlights)
                                SquidleAnnotationsData.objects.update_or_create(
                                    network=network,
                                    park=None,
                                    depth_zone=depth_zone['zonename'],
                                    highlights=highlights,
                                    defaults={
                                        "annotations_data": annotations_data
                                    }
                                )
                            except Exception as e:
                                logging.error(f"Error processing {network} > {depth_zone['zonename']} ({'Highlights' if highlights else 'No Highlights'})", exc_info=e)
                for highlights in [True, False]:
                    if skip_existing and SquidleAnnotationsData.objects.filter(network=network, park=None, depth_zone=None, highlights=highlights).exists():
                        logging.info(f"Skipping {network} > All Depths ({'Highlights' if highlights else 'No Highlights'})")
                    else:
                        try:
                            logging.info(f"Processing {network} > All Depths ({'Highlights' if highlights else 'No Highlights'})")
                            annotations_data = self.get_squidle_annotations(geojson, None, None, highlights)
                            SquidleAnnotationsData.objects.update_or_create(
                                network=network,
                                park=None,
                                depth_zone=None,
                                highlights=highlights,
                                defaults={
                                    "annotations_data": annotations_data
                                }
                            )
                        except Exception as e:
                            logging.error(f"Error processing {network} > All Depths ({'Highlights' if highlights else 'No Highlights'})", exc_info=e)

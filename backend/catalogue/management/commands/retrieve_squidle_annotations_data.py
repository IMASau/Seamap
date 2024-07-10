import requests
from requests.adapters import HTTPAdapter, Retry

from django.core.management.base import BaseCommand
from django.core.files.storage import default_storage
from django.core.files.base import ContentFile
from django.db import connections
from collections import namedtuple
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
        r = self.api.get("/api/annotation/tally/label")
        r.template("models/annotation/tally_chart_mini.html")
        r.results_per_page(10)

        # filters
        r.filter("point", "has", qf("media", "has", qf("poses", "any", qf("geom", "geo_in_mpolyh_xy", geojson))))
        if highlights:
            r.filter("point", "has", qf("media", "has", qf("annotations", "any", qf("annotations", "any", qf("tags", "any", qf("id", "eq", "348"))))))
        if min:
            r.filter("point", "has", qf("media", "has", qf("poses", "any", qf("dep", "gt", min))))
        if max:
            r.filter("point", "has", qf("media", "has", qf("poses", "any", qf("dep", "lte", max))))

        data = r.execute().text
        tree = PyQuery(data)
        if tree("div.tally-chart-row"):
            return str(tree("div.tally-chart"))
        else:
            return None


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
        self.api = SQAPI(host="https://squidle.org", api_key="28b75565b86a7218d211ad857d28477868b4924c1be1c75ebc9d2f9a")

        amp_depth_zones = AmpDepthZones.objects.all().values('netname', 'resname', 'zonename', 'min', 'max').distinct()
        network_region_depth_zones = {}
        for amp_depth_zone in amp_depth_zones:
            if amp_depth_zone['netname'] not in network_region_depth_zones:
                network_region_depth_zones[amp_depth_zone['netname']] = []
            network_region_depth_zones[amp_depth_zone['netname']].append({k: amp_depth_zone[k] for k in ['resname', 'zonename', 'min', 'max']})
        region_depth_zones = {}
        for network, depth_zones in network_region_depth_zones.items():
            if network not in region_depth_zones:
                region_depth_zones[network] = {}
            for depth_zone in depth_zones:
                if depth_zone['resname'] not in region_depth_zones[network]:
                    region_depth_zones[network][depth_zone['resname']] = []
                region_depth_zones[network][depth_zone['resname']].append({k: depth_zone[k] for k in ['zonename', 'min', 'max']})

        for network, region_depth_zone in region_depth_zones.items():
            for park, depth_zones in region_depth_zone.items():
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

        network_amp_depth_zones = AmpDepthZones.objects.all().values('netname', 'zonename', 'min', 'max').distinct()
        network_depth_zones = {}
        for network_amp_depth_zone in network_amp_depth_zones:
            if network_amp_depth_zone['netname'] not in network_depth_zones:
                network_depth_zones[network_amp_depth_zone['netname']] = []
            network_depth_zones[network_amp_depth_zone['netname']].append({k: network_amp_depth_zone[k] for k in ['zonename', 'min', 'max']})

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

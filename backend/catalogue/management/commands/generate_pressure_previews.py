from django.core.management.base import BaseCommand
from django.core.files.storage import default_storage
from django.core.files import File
from django.conf import settings
from urllib.request import urlopen
from urllib.parse import urlencode
from PIL import Image
from io import BytesIO
import logging

from catalogue.models import KeyedLayer, RegionReport, Pressure

Image.MAX_IMAGE_PIXELS = None

basemap = Image.open(default_storage.open('land_shallow_topo_21600.tif'))


def basemap_latitude_to_pixel(latitude):
    t = 0.5 + (latitude / 360)
    return round(basemap.width * t)


def basemap_longitude_to_pixel(longitude):
    t = 0.5 - (longitude / 180)
    return round(basemap.height * t)


def basemap_bbox(north, south, east, west):
    left = basemap_latitude_to_pixel(west)
    right = basemap_latitude_to_pixel(east)
    upper = basemap_longitude_to_pixel(north)
    lower = basemap_longitude_to_pixel(south)

    if (left > right):
        left_img = basemap.crop((left, upper, basemap.width + right, lower))
        right_img = basemap.crop((0, upper, right, lower))
        left_img.paste(right_img, (left_img.width - right, 0))
        return left_img
    else:
        return basemap.crop((left, upper, right, lower))

def get_layer_image(layer, bbox, size):
    sub_params = {
        'service': 'WMS',
        'request': 'GetMap',
        'layers': layer.layer_name,
        'styles': layer.style or '',
        'format': 'image/png',
        'transparent': 'true',
        'version': '1.1.1',
        'width': size['width'],
        'height': size['height'],
        'srs': 'EPSG:4326',
        'bbox': '{west},{south},{east},{north}'.format(**bbox)
    }

    url = f'{layer.server_url}?{urlencode(sub_params)}'
    logging.info(f'Retrieving layer image from: {url}')
    image = Image.open(urlopen(url)).convert('RGBA')
    logging.info('Layer image retrieval complete')
    return image

def get_boundary_layer_image(layer, bbox, size, network, park):
    sub_params = {
        'service': 'WMS',
        'request': 'GetMap',
        'layers': layer.layer_name,
        'styles': layer.style or '',
        'format': 'image/png',
        'transparent': 'true',
        'version': '1.1.1',
        'width': size['width'],
        'height': size['height'],
        'srs': 'EPSG:4326',
        'bbox': '{west},{south},{east},{north}'.format(**bbox),
        'cql_filter': f'RESNAME=\'{park}\'' if park else f'NETNAME=\'{network}\''
    }

    url = f'{layer.server_url}?{urlencode(sub_params)}'
    logging.info(f'Retrieving boundary layer image from: {url}')
    image = Image.open(urlopen(url)).convert('RGBA')
    logging.info('Boundary layer image retrieval complete')
    return image

def generate_pressure_preview(cropped_basemap, pressure, bbox, size):
    filepath = f'pressure_previews/{pressure.id}.png'
    logging.info(f'Generating pressure: {pressure}')
    layer_image = None
    try:
        layer_image = get_layer_image(pressure.layer, bbox, size)
    except Exception as e:
        logging.error('Error at %s', 'division', exc_info=e)
        logging.warn(f'Failed to retrieve image for {pressure.layer}')
    else:
        cropped_basemap.paste(layer_image, None, layer_image)
        with BytesIO() as bytes_io:
            cropped_basemap.save(bytes_io, 'PNG')
            default_storage.delete(filepath)
            default_storage.save(filepath, File(bytes_io, ''))

def get_bbox(region_report, target_ratio):
    region_bbox = bbox = {
        'north': region_report.maxy,
        'south': region_report.miny,
        'east':  region_report.maxx,
        'west':  region_report.minx
    }

    x_delta = (bbox['east'] - bbox['west'] + 360) % 360
    x_delta = x_delta if x_delta != 0 else 360
    y_delta = bbox['north'] - bbox['south']
    region_ratio = x_delta / y_delta

    if target_ratio > region_ratio:
        x_delta_diff = x_delta * ((target_ratio / region_ratio) - 1)
        return {
            'north': region_report.maxy,
            'south': region_report.miny,
            'east':  region_report.maxx + (x_delta_diff / 2),
            'west':  region_report.minx - (x_delta_diff / 2)
        }
    else:
        y_delta_diff = y_delta * ((region_ratio / target_ratio) - 1)
        return {
            'north': region_report.maxy + (y_delta_diff / 2),
            'south': region_report.miny - (y_delta_diff / 2),
            'east':  region_report.maxx,
            'west':  region_report.minx
        }

def generate_region_pressure_previews(region_report, boundary_layer):
    aspect_ratio = 5 / 4
    bbox = get_bbox(region_report, aspect_ratio)
    width = 386
    height = round(width / aspect_ratio)
    size = {
        'width': width,
        'height': height
    }

    cropped_basemap = basemap_bbox(**bbox).resize((width, height))

    boundary_layer_image = None

    try:
        boundary_layer_image = get_boundary_layer_image(boundary_layer, bbox, size, region_report.network, region_report.park)
    except Exception as e:
        logging.error('Error at %s', 'division', exc_info=e)
        logging.warn(f'Failed to retrieve boundary image for {boundary_layer}')
    else:
        cropped_basemap.paste(boundary_layer_image, None, boundary_layer_image)
        logging.info(f'Generating pressures for: {region_report}')
        for pressure in Pressure.objects.filter(region_report=region_report.id):
            generate_pressure_preview(cropped_basemap.copy(), pressure, bbox, size)

class Command(BaseCommand):
    def handle(self, *args, **options):
        boundary_layer = KeyedLayer.objects.get(keyword='data-report-minimap-panel1-boundary').layer
        for region_report in RegionReport.objects.all():
            generate_region_pressure_previews(region_report, boundary_layer)


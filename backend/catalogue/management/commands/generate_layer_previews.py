from django.core.management.base import BaseCommand
from django.core.files.storage import default_storage
from django.core.files import File
from django.conf import settings
from urllib.request import urlopen
from urllib.parse import urlencode
from PIL import Image
from io import BytesIO
import logging

from catalogue.models import Layer

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


def subdivide_requests(layer, horizontal_subdivisions=1, vertical_subdivisions=1):
    bbox = {
        'north': float(layer.maxy),
        'south': float(layer.miny),
        'east':  float(layer.maxx),
        'west':  float(layer.minx)
    }

    x_delta = bbox['east'] - bbox['west']
    y_delta = bbox['north'] - bbox['south']
    aspect_ratio = x_delta / y_delta
    width = 386
    height = round(width / aspect_ratio)

    urls = [None] * horizontal_subdivisions

    general_sub_width = width // horizontal_subdivisions
    general_x_delta = x_delta / width * general_sub_width
    general_sub_height = height // vertical_subdivisions
    general_y_delta = y_delta / height * general_sub_height

    for i in range(0, horizontal_subdivisions):
        urls[i] = [None] * vertical_subdivisions

        sub_width = general_sub_width
        sub_x_delta = general_x_delta
        if i == horizontal_subdivisions - 1:
            sub_width += width % horizontal_subdivisions
            sub_x_delta = x_delta / width * sub_width
        west = bbox['west'] + i * general_x_delta
        east = west + sub_x_delta

        for j in range(0, vertical_subdivisions):
            sub_height = general_sub_height
            sub_y_delta = general_y_delta
            if j == vertical_subdivisions - 1:
                sub_height += height % vertical_subdivisions
                sub_y_delta = y_delta / height * sub_height
            south = bbox['south'] + j * general_y_delta
            north = south + sub_y_delta

            sub_params = {
                'service': 'WMS',
                'request': 'GetMap',
                'layers': layer.layer_name,
                'styles': layer.style or '',
                'format': 'image/png',
                'transparent': 'true',
                'version': '1.1.1',
                'width': sub_width,
                'height': sub_height,
                'srs': 'EPSG:4326',
                'bbox': f'{west},{south},{east},{north}'
            }

            urls[i][j] = f'{layer.server_url}?{urlencode(sub_params)}'

    return urls

def retrieve_image(layer, horizontal_subdivisions=1, vertical_subdivisions=1):
    bbox = {
        'north': float(layer.maxy),
        'south': float(layer.miny),
        'east':  float(layer.maxx),
        'west': float(layer.minx)
    }

    x_delta = bbox['east'] - bbox['west']
    y_delta = bbox['north'] - bbox['south']
    aspect_ratio = x_delta / y_delta
    width = 386
    height = round(width / aspect_ratio)

    cropped_basemap = basemap_bbox(**bbox).resize((width, height))

    urls = subdivide_requests(layer, horizontal_subdivisions, vertical_subdivisions)
    for i, h_url in enumerate(urls):
        for j, url in enumerate(h_url):
            logging.info(f'Retrieving part {i * vertical_subdivisions + j + 1}/{horizontal_subdivisions * vertical_subdivisions} from {url}')
            layer_image = Image.open(urlopen(url)).convert('RGBA')
            cropped_basemap.paste(
                layer_image,
                (
                    (width // horizontal_subdivisions) * i,
                    height - (height // vertical_subdivisions) * j - layer_image.height
                ),
                layer_image
            )

    return cropped_basemap


class Command(BaseCommand):
    def add_arguments(self, parser):
        parser.add_argument(
            '--layer_id',
            help='Specify a layer ID for the layer preview image you want to generate'
        )
        parser.add_argument(
            '--only_generate_missing',
            help='If true, skips generating images for layers where images already exist'
        )
        parser.add_argument(
            '--horizontal_subdivisions',
            help='Number of columns to break the GetMap query for each layer into'
        )
        parser.add_argument(
            '--vertical_subdivisions',
            help='Number of rows to break the GetMap query for each layer into'
        )

    def handle(self, *args, **options):
        layer_id = int(options['layer_id']) if options['layer_id'] != None else None
        only_generate_missing = options['only_generate_missing'].lower() in ['t', 'true'] if options['only_generate_missing'] != None else False
        horizontal_subdivisions = int(options['horizontal_subdivisions']) if options['horizontal_subdivisions'] != None else 1
        vertical_subdivisions = int(options['vertical_subdivisions']) if options['vertical_subdivisions'] != None else 1

        if layer_id is not None:
            layer = Layer.objects.get(id=layer_id)

            filepath = f'layer_previews/{layer.id}.png'
            if not only_generate_missing or not default_storage.exists(filepath):
                with BytesIO() as bytes_io:
                    layer_image = retrieve_image(layer, horizontal_subdivisions, vertical_subdivisions)
                    layer_image.save(bytes_io, 'PNG')

                    default_storage.delete(filepath)
                    default_storage.save(filepath, File(bytes_io, ''))
        else:
            failures = []

            for layer in Layer.objects.all():
                filepath = f'layer_previews/{layer.id}.png'
                if not only_generate_missing or not default_storage.exists(filepath):
                    with BytesIO() as bytes_io:
                        try:
                            logging.info(f'Retrieving layer {layer.layer_name} ({layer.id})')
                            layer_image = retrieve_image(layer, horizontal_subdivisions, vertical_subdivisions)
                            layer_image.save(bytes_io, 'PNG')
                            
                            default_storage.delete(filepath)
                            default_storage.save(filepath, File(bytes_io, ''))
                        except Exception:
                            logging.warn(f'Failed to retrieve layer {layer.layer_name} ({layer.id})')
                            failures.append(layer)

            if len(failures):
                logging.warn('Failed to retrieve the following layers: \n - {}'.format(
                    '\n - '.join(
                        [f'{layer.layer_name} ({layer.id})' for layer in failures]
                    )
                ))

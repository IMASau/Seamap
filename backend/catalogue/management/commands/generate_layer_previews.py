from django.core.management.base import BaseCommand
from django.core.files.storage import default_storage
from django.core.files import File
from django.conf import settings
from urllib.request import urlopen
from urllib.parse import urlencode
from PIL import Image
from io import BytesIO

from catalogue.models import Layer


basemap = Image.open(default_storage.open('basemap.png'))


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


def retrieve_image(layer):
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

    params = {
        'service': 'WMS',
        'request': 'GetMap',
        'layers': layer.layer_name,
        'styles': '',
        'format': 'image/png',
        'transparent': 'true',
        'version': '1.1.1',
        'width': width,
        'height': height,
        'srs': 'EPSG:4326',
        'bbox': '{west},{south},{east},{north}'.format(**bbox)
    }

    url = '{server_url}?{params}'.format(
        server_url=layer.server_url, params=urlencode(params))

    layer_image = Image.open(urlopen(url)).convert('RGBA')
    cropped_basemap = basemap_bbox(**bbox).resize((width, height))
    cropped_basemap.paste(layer_image, None, layer_image)
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

    def handle(self, *args, **options):
        layer_id = int(options['layer_id']) if options['layer_id'] != None else None
        only_generate_missing = options['only_generate_missing'].lower() in ['t', 'true'] if options['only_generate_missing'] != None else False



        if layer_id is not None:
            layer = Layer.objects.get(id=layer_id)

            filepath = 'layer_previews/{}.png'.format(layer.id)
            if not only_generate_missing or not default_storage.exists(filepath):
                bytes_io = BytesIO()

                layer_image = retrieve_image(layer)
                layer_image.save(bytes_io, 'PNG')

                default_storage.delete(filepath)
                default_storage.save(filepath, File(bytes_io, ''))

                bytes_io.close()
        else:
            failures = []

            for layer in Layer.objects.all():
                filepath = 'layer_previews/{}.png'.format(layer.id)
                if not only_generate_missing or not default_storage.exists(filepath):
                    bytes_io = BytesIO()

                    try:
                        layer_image = retrieve_image(layer)
                        layer_image.save(bytes_io, 'PNG')
                        
                        default_storage.delete(filepath)
                        default_storage.save(filepath, File(bytes_io, ''))
                    except Exception:
                        self.stdout.write('Failed to retrieve layer {layer_name} ({id})'.format(
                            layer_name=layer.layer_name, id=layer.id))
                        failures.append(layer)

                    bytes_io.close()

            if len(failures):
                self.stdout.write('Failed to retrieve the following layers: \n - {}'.format('\n - '.join(
                    ['{layer_name} ({id})'.format(layer_name=layer.layer_name, id=layer.id) for layer in failures])))

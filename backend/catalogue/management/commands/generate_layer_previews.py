from django.core.management.base import BaseCommand
from django.core.files.storage import default_storage
from django.core.files import File
from urllib.request import urlopen
from urllib.parse import urlencode
from urllib.error import HTTPError
from PIL import Image, UnidentifiedImageError
from io import BytesIO
import logging
import requests
import geopandas
import geoplot
import matplotlib.pyplot as plt
from catalogue.emails import email_generate_layer_preview_summary

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

    x_delta = (bbox['east'] - bbox['west'] + 360) % 360
    x_delta = x_delta if x_delta != 0 else 360
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

def retrieve_image(layer, horizontal_subdivisions=None, vertical_subdivisions=None):
    horizontal_subdivisions = horizontal_subdivisions or 1
    vertical_subdivisions = vertical_subdivisions or 1

    bbox = {
        'north': float(layer.maxy),
        'south': float(layer.miny),
        'east':  float(layer.maxx),
        'west': float(layer.minx)
    }

    x_delta = (bbox['east'] - bbox['west'] + 360) % 360
    x_delta = x_delta if x_delta != 0 else 360
    y_delta = bbox['north'] - bbox['south']
    aspect_ratio = x_delta / y_delta
    width = 386
    height = round(width / aspect_ratio)

    cropped_basemap = basemap_bbox(**bbox).resize((width, height))

    urls = subdivide_requests(layer, horizontal_subdivisions, vertical_subdivisions)

    for i, h_urls in enumerate(urls):
        for j, url in enumerate(h_urls):
            try:
                response = urlopen(url)
            except HTTPError as e:
                raise Exception(f"URL {url} returned an error response") from e

            try:
                layer_image = Image.open(response)
            except UnidentifiedImageError as e:
                raise Exception(f"Response from URL {url} could not be converted to an image") from e

            layer_image = layer_image.convert('RGBA')

            cropped_basemap.paste(
                layer_image,
                (
                    (width // horizontal_subdivisions) * i,
                    height - (height // vertical_subdivisions) * j - layer_image.height
                ),
                layer_image
            )
    return cropped_basemap

def generate_layer_preview(layer, only_generate_missing, horizontal_subdivisions, vertical_subdivisions):
    """
    Generate and save a layer preview.
    """
    filepath = f'layer_previews/{layer.id}.png'
    if not only_generate_missing or not default_storage.exists(filepath):
        with BytesIO() as bytes_io:
            if horizontal_subdivisions or vertical_subdivisions:
                try:
                    layer_image = retrieve_image(layer, horizontal_subdivisions, vertical_subdivisions)
                except Exception as e:
                    raise Exception(f"Could not retrieve image in {horizontal_subdivisions}x{vertical_subdivisions} subdivisions") from e
            else:
                try:
                    layer_image = retrieve_image(layer)
                except Exception as e:
                    try:
                        layer_image = retrieve_image(layer, 40, 40)
                    except Exception as e:
                        raise Exception("Could not retrieve image in single request or in 40x40 subdivisions") from e

            layer_image.save(bytes_io, 'PNG')
            default_storage.delete(filepath)
            default_storage.save(filepath, File(bytes_io, ''))

def featureserver_layer_preview():
    url = "https://services1.arcgis.com/wfNKYeHsOyaFyPw3/arcgis/rest/services/Declared_Area_OEI_01_2022/FeatureServer/0/query"
    params = {
        'where':        '1=1',
        'outFields':    '*', # useful when renderer type = uniqueValue
        'f':            'geojson',
        'resultOffset': 0
    }
    r = requests.get(url=url, params=params)
    collection = r.json()
    features = collection['features']
    for feature in features:
        feature['properties'] = []
    gdf = geopandas.GeoDataFrame.from_features(features)
    geoplot.polyplot(
        gdf,
        facecolor=(0.298, 0.506, 0.804, 0.75), # need to split features into separate data frames with facecolor and edgecolor from drawingInfo in separate query
        edgecolor=(0, 0, 0, 1),
        linewidth=0.75,
        extent=gdf.total_bounds
    )
    plt.savefig("test.png", transparent=True, bbox_inches='tight')
    bounds = gdf.total_bounds
    bmap = basemap_bbox(bounds[3], bounds[1], bounds[2], bounds[0])
    bmap.save("test1.png")

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
        horizontal_subdivisions = int(options['horizontal_subdivisions']) if options['horizontal_subdivisions'] != None else None
        vertical_subdivisions = int(options['vertical_subdivisions']) if options['vertical_subdivisions'] != None else None
        errors = []

        if layer_id is not None:
            layer = Layer.objects.get(id=layer_id)
            try:
                generate_layer_preview(layer, only_generate_missing, horizontal_subdivisions, vertical_subdivisions)
            except Exception as e:
                logging.error(f"Error processing layer {layer.id}", exc_info=e)
                errors.append({'layer': layer, 'e': e})
        else:
            for layer in Layer.objects.all():
                try:
                    generate_layer_preview(layer, only_generate_missing, horizontal_subdivisions, vertical_subdivisions)
                except Exception as e:
                    logging.error(f"Error processing layer {layer.id}", exc_info=e)
                    errors.append({'layer': layer, 'e': e})

        if len(errors):
            logging.warn("Failed to retrieve the following layers: \n{}".format(
                '\n'.join(
                    [f" • {error['layer'].layer_name} ({error['layer'].id})" for error in errors]
                )
            ))

            if not layer_id:
                email_generate_layer_preview_summary(errors)

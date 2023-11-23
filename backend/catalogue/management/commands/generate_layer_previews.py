from django.core.management.base import BaseCommand
from django.core.files.storage import default_storage
from django.core.files import File
from urllib.request import urlopen
from urllib.parse import urlencode
from urllib.error import HTTPError
from PIL import Image, UnidentifiedImageError
from io import BytesIO
import logging
import re
import requests
import geopandas
import geoplot
import matplotlib.pyplot as plt
from catalogue.emails import email_generate_layer_preview_summary

from catalogue.models import Layer

Image.MAX_IMAGE_PIXELS = None

basemap = Image.open(default_storage.open('land_shallow_topo_21600.tif'))

def bounds_to_image_info(bounds):
    x_delta = (bounds['east'] - bounds['west'] + 360) % 360
    x_delta = x_delta if x_delta != 0 else 360
    y_delta = bounds['north'] - bounds['south']
    aspect_ratio = x_delta / y_delta
    width = 386
    height = round(width / aspect_ratio)

    return {
        'width': width,
        'height': height,
        'x_delta': x_delta,
        'y_delta': y_delta,
        'aspect_ratio': aspect_ratio
    }

def basemap_latitude_to_pixel(latitude):
    t = 0.5 + (latitude / 360)
    return round(basemap.width * t)


def basemap_longitude_to_pixel(longitude):
    t = 0.5 - (longitude / 180)
    return round(basemap.height * t)


def cropped_basemap_image(north, south, east, west) -> Image:
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


def subdivide_requests(layer: Layer, horizontal_subdivisions: int=1, vertical_subdivisions: int=1) -> list[list[str]]:
    bounds = layer.bounds()
    image_info = bounds_to_image_info(bounds)

    urls = [None] * horizontal_subdivisions

    general_sub_width = image_info['width'] // horizontal_subdivisions
    general_x_delta = image_info['x_delta'] / image_info['width'] * general_sub_width
    general_sub_height = image_info['height'] // vertical_subdivisions
    general_y_delta = image_info['y_delta'] / image_info['height'] * general_sub_height

    for i in range(0, horizontal_subdivisions):
        urls[i] = [None] * vertical_subdivisions

        sub_width = general_sub_width
        sub_x_delta = general_x_delta
        if i == horizontal_subdivisions - 1:
            sub_width += image_info['width'] % horizontal_subdivisions
            sub_x_delta = image_info['x_delta'] / image_info['width'] * sub_width
        west = bounds['west'] + i * general_x_delta
        east = west + sub_x_delta

        for j in range(0, vertical_subdivisions):
            sub_height = general_sub_height
            sub_y_delta = general_y_delta
            if j == vertical_subdivisions - 1:
                sub_height += image_info['height'] % vertical_subdivisions
                sub_y_delta = image_info['y_delta'] / image_info['height'] * sub_height
            south = bounds['south'] + j * general_y_delta
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

def geoserver_retrieve_image(layer: Layer, horizontal_subdivisions: int=1, vertical_subdivisions: int=1) -> Image:
    bounds = layer.bounds()
    image_info = bounds_to_image_info(bounds)

    image =  Image.new('RGBA', (image_info['width'], image_info['height']))

    urls = subdivide_requests(layer, horizontal_subdivisions, vertical_subdivisions)

    for i, h_urls in enumerate(urls):
        for j, url in enumerate(h_urls):
            try:
                response = urlopen(url)
            except HTTPError as e:
                raise Exception(f"URL {url} returned an error response") from e

            try:
                sub_image = Image.open(response)
            except UnidentifiedImageError as e:
                raise Exception(f"Response from URL {url} could not be converted to an image") from e

            sub_image = sub_image.convert('RGBA')

            image.paste(
                sub_image,
                (
                    (image_info['width'] // horizontal_subdivisions) * i,
                    image_info['height'] - (image_info['height'] // vertical_subdivisions) * j - sub_image.height
                ),
                sub_image
            )
    return image

def geoserver_layer_image(layer: Layer, horizontal_subdivisions: int | None, vertical_subdivisions: int | None) -> Image:
    if horizontal_subdivisions or vertical_subdivisions:
        try:
            return geoserver_retrieve_image(layer, horizontal_subdivisions, vertical_subdivisions)
        except Exception as e:
            raise Exception(f"Could not retrieve image in {horizontal_subdivisions}x{vertical_subdivisions} subdivisions") from e
    else:
        try:
            return geoserver_retrieve_image(layer)
        except Exception as e:
            try:
                return geoserver_retrieve_image(layer, 40, 40)
            except Exception as e:
                raise Exception("Could not retrieve image in single request or in 40x40 subdivisions") from e

def symbol_to_geoplot_args(symbol):
    facecolor = symbol.get('color')
    if facecolor:
        facecolor = list(map(lambda v: v / 255, facecolor))

    edgecolor = symbol.get('outline').get('color')
    if edgecolor:
        edgecolor = list(map(lambda v: v / 255, edgecolor))

    linewidth = symbol.get('outline').get('width')

    return {
        'facecolor': facecolor,
        'edgecolor': edgecolor,
        'linewidth': linewidth
    }

def mapserver_layer_image(layer: Layer) -> Image:
    drawing_info = layer.server_info()['drawingInfo']
    renderer_type = drawing_info['renderer']['type']
    bounds = layer.bounds()
    image_info = bounds_to_image_info(bounds)

    if renderer_type == 'simple':
        geojson = layer.geojson()
    elif renderer_type == 'uniqueValue':
        geojson = layer.geojson('*')
    else:
        raise ValueError(f"renderer_type '{renderer_type}' not handled")
    
    for feature in geojson['features']:
        feature['properties'] = feature['properties'] or {}

    gdf = geopandas.GeoDataFrame.from_features(geojson['features'])
    gdf.columns = gdf.columns.str.lower()
    ax = None

    if renderer_type == 'simple':
        geoplot_args = symbol_to_geoplot_args(drawing_info['renderer']['symbol'])

        ax = geoplot.polyplot(
            gdf,
            ax=ax,
            extent=[bounds['west'], bounds['south'], bounds['east'], bounds['north']],
            **geoplot_args
        )
    elif renderer_type == 'uniqueValue':
        value_column = drawing_info['renderer']['field1'].lower()
        unique_value_infos = drawing_info['renderer']['uniqueValueInfos']

        for unique_value_info in unique_value_infos:
            geoplot_args = symbol_to_geoplot_args(unique_value_info['symbol'])
            filtered_gdf = gdf[gdf[value_column] == unique_value_info['value']]

            ax = geoplot.polyplot(
                filtered_gdf,
                ax=ax,
                extent=[bounds['west'], bounds['south'], bounds['east'], bounds['north']],
                **geoplot_args
            )

    with BytesIO() as bytes_io:
        plt.plot(ax=ax)
        plt.tight_layout()
        plt.gcf().set_size_inches(image_info['width'], image_info['height'])
        plt.savefig(
            bytes_io,
            format='png',
            transparent=True,
            dpi=1
        )
        bytes_io.seek(0)
        image = Image.open(bytes_io).copy()
        return image

def featureserver_layer_image(layer: Layer) -> Image:
    return mapserver_layer_image(layer) # FeatureServer case seemingly the same as MapServer case (for now)

def generate_layer_preview(layer: Layer, horizontal_subdivisions: int | None, vertical_subdivisions: int | None) -> None:
    filepath = f'layer_previews/{layer.id}.png'
    bounds = layer.bounds()
    image_info = bounds_to_image_info(bounds)

    layer_image = None
    if re.search(r'^(.+?)/services/(.+?)/MapServer/.+$', layer.server_url):
        layer_image = mapserver_layer_image(layer)
    elif re.search(r'^(.+?)/services/(.+?)/FeatureServer/.+$', layer.server_url):
        layer_image = featureserver_layer_image(layer)
    else:
        layer_image = geoserver_layer_image(layer, horizontal_subdivisions, vertical_subdivisions)

    cropped_basemap = cropped_basemap_image(**bounds).resize((image_info['width'], image_info['height']))
    cropped_basemap.paste(layer_image, mask=layer_image)

    with BytesIO() as bytes_io:
        cropped_basemap.save(bytes_io, 'PNG')
        default_storage.delete(filepath)
        default_storage.save(filepath, File(bytes_io, ''))

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
            filepath = f'layer_previews/{layer.id}.png'

            if not only_generate_missing or not default_storage.exists(filepath):
                try:
                    generate_layer_preview(layer, horizontal_subdivisions, vertical_subdivisions)
                except Exception as e:
                    logging.error(f"Error processing layer {layer.id}", exc_info=e)
                    errors.append({'layer': layer, 'e': e})
        else:
            for layer in Layer.objects.all():
                filepath = f'layer_previews/{layer.id}.png'

                if not only_generate_missing or not default_storage.exists(filepath):
                    try:
                        generate_layer_preview(layer, horizontal_subdivisions, vertical_subdivisions)
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

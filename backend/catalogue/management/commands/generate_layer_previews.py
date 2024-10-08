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
import geopandas
import geoplot
import matplotlib.pyplot as plt
import base64
import matplotlib.image as mpimg
import functools
from matplotlib.image import BboxImage
from matplotlib.transforms import Bbox, TransformedBbox
from shapely.geometry import shape, LineString, MultiLineString, Polygon, MultiPolygon
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

def wms_layer_image(layer: Layer, horizontal_subdivisions: int, vertical_subdivisions: int) -> Image:
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

def drawing_info_to_opacity(drawing_info) -> float:
    return (100 - drawing_info.get('transparency', 0)) / 100

def symbol_to_geoplot_args(symbol, opacity: float):
    plot_type = symbol_to_plot_type(symbol)

    facecolor = None
    if plot_type == 'polygon':
        facecolor = symbol.get('color')
    if facecolor:
        facecolor = list(map(lambda v: v / 255, facecolor))
        facecolor[3] = facecolor[3] * opacity

    edgecolor = None
    if plot_type == 'polygon':
        edgecolor = (symbol.get('outline') or {}).get('color')
    elif plot_type == 'line':
        edgecolor = symbol.get('color')
    if edgecolor:
        edgecolor = list(map(lambda v: v / 255, edgecolor))
        edgecolor[3] = edgecolor[3] * opacity

    linewidth = (symbol.get('outline') or {}).get('width')

    hatch_types = {
        'esriSFSBackwardDiagonal': '\\',
        'esriSFSCross': '+',
        'esriSFSDiagonalCross': 'x',
        'esriSFSForwardDiagonal': '/',
        'esriSFSHorizontal': '-',
        'esriSFSVertical': '|'
    }
    hatch = hatch_types.get(symbol.get('style'))

    color = None
    if symbol.get('imageData'):
        color = 'None'

    if hatch:
        edgecolor = edgecolor or facecolor
        facecolor = 'None'

    return {
        'facecolor': facecolor,
        'edgecolor': edgecolor,
        'linewidth': linewidth,
        'hatch': hatch,
        'color': color
    }

def symbol_to_plot_type(symbol) -> str:
    plot_type = symbol['type']
    if plot_type == 'esriSFS':
        return 'polygon'
    elif plot_type == 'esriSLS':
        return 'line'
    elif plot_type == 'esriPMS':
        return 'point'
    else:
        raise ValueError(f"plot_type '{plot_type}' not handled")

def symbol_to_marker_image(symbol):
    if symbol.get('imageData'):
        i = base64.b64decode(symbol['imageData'])
        i = BytesIO(i)
        return mpimg.imread(i, format=symbol['imageData'].split('/')[-1])

def add_marker_image_point(ax, marker_image, point):
    bb = Bbox.from_bounds(point.x-0.5, point.y-0.5, 1, 1)  
    bb2 = TransformedBbox(bb, ax.transData)
    bbox_image = BboxImage(
        bb2,
        norm=None,
        origin=None,
        clip_on=False
    )
    bbox_image.set_data(marker_image)
    ax.add_artist(bbox_image)

def linestring_to_polygon(geometry: LineString) -> Polygon:
    coords = list(geometry.coords)
    if len(coords) == 2:
        return Polygon(coords + [coords[0]])
    else:
        return Polygon(coords + list(reversed(coords[1:-1])))


def geometry_to_polygon(geometry: shape) -> shape:
    if isinstance(geometry, LineString):
        return linestring_to_polygon(geometry)
    elif isinstance(geometry, MultiLineString):
        return MultiPolygon(linestring_to_polygon(v) for v in geometry.geoms)
    return geometry

def compare_unique_value_infos(unique_value_info1, unique_value_info2) -> int:
    style1 = unique_value_info1['symbol'].get('style')
    style2 = unique_value_info2['symbol'].get('style')

    try:
        opacity1 = unique_value_info1['symbol']['color'][3]
    except:
        opacity1 = 255
    try:
        opacity2 = unique_value_info2['symbol']['color'][3]
    except:
        opacity2 = 255

    if style1 == 'esriSFSSolid' and style2 != 'esriSFSSolid':
        return -1
    elif style1 != 'esriSFSSolid' and style2 == 'esriSFSSolid':
        return 1

    return opacity1 - opacity2

def mapserver_vector_layer_image(layer: Layer) -> Image:
    drawing_info = layer.server_info()['drawingInfo']
    opacity = drawing_info_to_opacity(drawing_info)
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
        symbol = drawing_info['renderer']['symbol']
        geoplot_args = symbol_to_geoplot_args(symbol, opacity)
        plot_type = symbol_to_plot_type(symbol)
        marker_image = symbol_to_marker_image(symbol)

        if plot_type == 'polygon':
            ax = geoplot.polyplot(
                gdf,
                ax=ax,
                extent=[bounds['west'], bounds['south'], bounds['east'], bounds['north']],
                **geoplot_args
            )
        elif plot_type == 'line':
            gdf['geometry'] = gdf['geometry'].map(geometry_to_polygon)
            ax = geoplot.polyplot(
                gdf,
                ax=ax,
                extent=[bounds['west'], bounds['south'], bounds['east'], bounds['north']],
                **geoplot_args
            )
        elif plot_type == 'point':
            ax = geoplot.pointplot(
                gdf,
                ax=ax,
                extent=[bounds['west'], bounds['south'], bounds['east'], bounds['north']],
                **geoplot_args
            )

            if symbol.get('imageData'):
                for i, row in gdf.iterrows():
                    add_marker_image_point(ax, marker_image, row['geometry'])
        else:
            raise ValueError(f"plot_type '{plot_type}' not handled")
    elif renderer_type == 'uniqueValue':
        value_column = drawing_info['renderer']['field1'].lower()
        gdf[value_column] = gdf[value_column].astype(str)
        unique_value_infos = drawing_info['renderer']['uniqueValueInfos']
        unique_value_infos.sort(key=functools.cmp_to_key(compare_unique_value_infos))

        for unique_value_info in unique_value_infos:
            symbol = unique_value_info['symbol']
            geoplot_args = symbol_to_geoplot_args(symbol, opacity)
            plot_type = symbol_to_plot_type(symbol)
            marker_image = symbol_to_marker_image(symbol)
            filtered_gdf = gdf[gdf[value_column] == unique_value_info['value']]

            if plot_type == 'polygon':
                ax = geoplot.polyplot(
                    filtered_gdf,
                    ax=ax,
                    extent=[bounds['west'], bounds['south'], bounds['east'], bounds['north']],
                    **geoplot_args
                )
            elif plot_type == 'line':
                filtered_gdf['geometry'] = filtered_gdf['geometry'].map(geometry_to_polygon)
                ax = geoplot.polyplot(
                    filtered_gdf,
                    ax=ax,
                    extent=[bounds['west'], bounds['south'], bounds['east'], bounds['north']],
                    **geoplot_args
                )
            elif plot_type == 'point':
                ax = geoplot.pointplot(
                    filtered_gdf,
                    ax=ax,
                    extent=[bounds['west'], bounds['south'], bounds['east'], bounds['north']],
                    **geoplot_args
                )

                if symbol.get('imageData'):
                    for i, row in filtered_gdf.iterrows():
                        add_marker_image_point(ax, marker_image, row['geometry'])
            else:
                raise ValueError(f"plot_type '{plot_type}' not handled")
    else:
        raise ValueError(f"renderer_type '{renderer_type}' not handled")

    with BytesIO() as bytes_io:
        plt.plot(ax=ax)
        plt.tight_layout(pad=0)
        fig = plt.gcf()
        fig_width = fig.get_size_inches()[0]
        fig.set_size_inches(fig_width, fig_width / image_info['aspect_ratio'])
        plt.savefig(
            bytes_io,
            format='png',
            transparent=True
        )
        bytes_io.seek(0)
        image = Image.open(bytes_io).copy()
        return image

def featureserver_vector_layer_image(layer: Layer) -> Image:
    return mapserver_vector_layer_image(layer) # FeatureServer case seemingly the same as MapServer case (for now)

def generate_layer_preview(layer: Layer, horizontal_subdivisions: int, vertical_subdivisions: int) -> None:
    filepath = f'layer_previews/{layer.id}.png'
    bounds = layer.bounds()
    image_info = bounds_to_image_info(bounds)

    layer_image = None
    if re.search(r'^(.+?)/services/(.+?)/MapServer/(?!WMSServer).+$', layer.server_url):
        layer_image = mapserver_vector_layer_image(layer)
    elif re.search(r'^(.+?)/services/(.+?)/FeatureServer/(?!WMSServer).+$', layer.server_url):
        layer_image = featureserver_vector_layer_image(layer)
    else:
        layer_image = wms_layer_image(layer, horizontal_subdivisions, vertical_subdivisions)

    layer_image = layer_image.resize((image_info['width'], image_info['height']))
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
            '--skip_existing',
            help='If false, skips generating images for layers where images already exist'
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
        skip_existing = options['skip_existing'].lower() in ['t', 'true'] if options['skip_existing'] != None else False
        horizontal_subdivisions = int(options['horizontal_subdivisions']) if options['horizontal_subdivisions'] != None else None
        vertical_subdivisions = int(options['vertical_subdivisions']) if options['vertical_subdivisions'] != None else None
        errors = []

        if layer_id is not None:
            layer = Layer.objects.get(id=layer_id)
            filepath = f'layer_previews/{layer.id}.png'

            if not skip_existing or not default_storage.exists(filepath):
                try:
                    generate_layer_preview(layer, horizontal_subdivisions, vertical_subdivisions)
                except Exception as e:
                    logging.error(f"Error processing layer {layer.id}", exc_info=e)
                    errors.append({'layer': layer, 'e': e})
        else:
            for layer in Layer.objects.all():
                filepath = f'layer_previews/{layer.id}.png'
                exists = default_storage.exists(filepath)

                # Always generate a preview if it doesn't exist.
                # If a preview does exist, only generate it if the layer has regenerate_preview
                # set to True and we're not skipping existing previews.
                if not exists or (layer.regenerate_preview and not skip_existing):
                    try:
                        generate_layer_preview(layer, horizontal_subdivisions, vertical_subdivisions)
                    except Exception as e:
                        logging.error(f"Error processing layer {layer.id}", exc_info=e)
                        errors.append({'layer': layer, 'e': e})

        if len(errors):
            logging.warn("Failed to retrieve the following layers: \n{}".format(
                '\n'.join(
                    [f" • {error['layer'].name} ({error['layer'].id})" for error in errors]
                )
            ))

            if not layer_id:
                email_generate_layer_preview_summary(errors)

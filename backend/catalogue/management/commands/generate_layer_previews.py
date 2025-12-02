# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.

"""
Management command to generate layer previews.
"""
import base64
import functools
import logging
import re
from io import BytesIO
from urllib.error import HTTPError
from urllib.parse import urlencode
from urllib.request import urlopen

import geopandas
import geoplot
import matplotlib.image as mpimg
import matplotlib.pyplot as plt
import numpy
import rasterio
from rasterio import transform, warp
from pyproj import Transformer
from catalogue.emails import email_generate_layer_preview_summary
from catalogue.models import Layer
from django.core.files.storage import default_storage
from django.core.management.base import BaseCommand
from matplotlib.image import BboxImage
from matplotlib.transforms import Bbox, TransformedBbox
from PIL import Image, UnidentifiedImageError
from shapely.geometry import (LineString, MultiLineString, MultiPolygon,
                              Polygon, shape)

# pylint: disable=line-too-long
# pylint: disable=missing-function-docstring
# pylint: disable=missing-class-docstring
# pylint: disable=redefined-outer-name

Image.MAX_IMAGE_PIXELS = None

# We not able to simply *add* the basemap image file to our GitHub tracking, as it's too large.
# For now, the image will be added to deployments manually.
BASEMAP_FILEPATH = default_storage.path('land_shallow_topo_21600.tif')
BASEMAP_CRS = 'EPSG:4326'
BASEMAP_BOUNDS = (-180, -90, 180, 90)
DST_WIDTH = 386

def get_image_details_from_projected_layer_bounds(min_x: float, min_y: float, max_x: float, max_y: float) -> tuple:
    """
    Get image details from the projected layer bounds.

    Args:
        min_x (float): Minimum x coordinate
        min_y (float): Minimum y coordinate
        max_x (float): Maximum x coordinate
        max_y (float): Maximum y coordinate

    Returns:
        tuple: (width, height, x_delta, y_delta, aspect_ratio)
    """
    x_delta = max_x - min_x
    y_delta = max_y - min_y
    aspect_ratio = x_delta / y_delta
    height = round(DST_WIDTH / aspect_ratio)
    return ( DST_WIDTH, height, x_delta, y_delta, aspect_ratio )


def get_projected_layer_bounds(layer: Layer, target_crs: str) -> tuple:
    """
    Get the projected bounds of the layer in the target CRS.

    Args:
        layer (Layer): The layer to get the bounds for.
        target_crs (str): The target coordinate reference system (e.g. "EPSG:3031")

    Returns:
        tuple: The projected bounds as (min_x, min_y, max_x, max_y)
    """
    transformer = Transformer.from_crs('EPSG:4326', target_crs, always_xy=True)
    bounds = layer.bounds()
    return transformer.transform_bounds(bounds['west'], bounds['south'], bounds['east'], bounds['north'])


def get_basemap_cropped_basemap_image(min_x: float, min_y: float, max_x: float, max_y: float, target_crs: str) -> Image:
    """
    Generate a cropped basemap image in the target CRS.

    Args:
        min_x (float): Minimum x coordinate (in target CRS)
        min_y (float): Minimum y coordinate (in target CRS)
        max_x (float): Maximum x coordinate (in target CRS)
        max_y (float): Maximum y coordinate (in target CRS)
        target_crs (str): Target coordinate reference system (e.g. "EPSG:3031")

    Returns:
        Image: Cropped basemap image in the target CRS.
    """
    dst_resolution = (max_x - min_x) / DST_WIDTH
    dst_height = int((max_y - min_y) / dst_resolution)
    dst_transform = transform.from_bounds(min_x, min_y, max_x, max_y, DST_WIDTH, dst_height)
    with rasterio.open(BASEMAP_FILEPATH) as basemap_src:
        src_crs = rasterio.crs.CRS.from_string(BASEMAP_CRS)
        src_transform = transform.from_bounds(*BASEMAP_BOUNDS, width=basemap_src.width, height=basemap_src.height)
        profile = basemap_src.profile.copy()
        profile.update({
            "crs": target_crs,
            "transform": dst_transform,
            "width": DST_WIDTH,
            "height": dst_height,
        })
        dst_arrays = []
        for i in range(1, basemap_src.count + 1): # Iterate over each band (e.g. R, G, B, A)
            src_array = basemap_src.read(i)
            dst_array = numpy.empty((dst_height, DST_WIDTH), dtype=src_array.dtype)

            warp.reproject(
                src_array,
                dst_array,
                src_transform=src_transform,
                src_crs=src_crs,
                dst_transform=dst_transform,
                dst_crs=target_crs,
                resampling=warp.Resampling.bilinear,
            )

            dst_arrays.append(dst_array)

        # Create PIL Image from the reprojected data
        mode_map = {1: 'L', 3: 'RGB', 4: 'RGBA'}
        mode = mode_map.get(basemap_src.count, 'L')
        if basemap_src.count == 1:
            bands_array = dst_arrays[0].astype('uint8')
        else:
            bands_array = numpy.dstack(dst_arrays).astype('uint8')
        return Image.fromarray(bands_array, mode=mode)


def subdivide_requests(layer: Layer, target_crs: str, horizontal_subdivisions: int=1, vertical_subdivisions: int=1) -> list[list[str]]:
    """
    Subdivide WMS GetMap requests into smaller requests.

    Args:
        layer (Layer): The layer to generate requests for.
        target_crs (str): Target coordinate reference system (e.g. 'EPSG:3031')
        horizontal_subdivisions (int): Number of horizontal subdivisions.
        vertical_subdivisions (int): Number of vertical subdivisions.

    Returns:
        list[list[str]]: A 2D list of URLs for the subdivided requests.
    """
    ( min_x, min_y, max_x, max_y ) = get_projected_layer_bounds(layer, target_crs)
    ( width, height, x_delta, y_delta, _ ) = get_image_details_from_projected_layer_bounds(min_x, min_y, max_x, max_y)

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
        sub_min_x = min_x + i * general_x_delta
        sub_max_x = sub_min_x + sub_x_delta

        for j in range(0, vertical_subdivisions):
            sub_height = general_sub_height
            sub_y_delta = general_y_delta
            if j == vertical_subdivisions - 1:
                sub_height += height % vertical_subdivisions
                sub_y_delta = y_delta / height * sub_height
            sub_min_y = min_y + j * general_y_delta
            sub_max_y = sub_min_y + sub_y_delta

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
                'srs': target_crs,
                'bbox': f'{sub_min_x},{sub_min_y},{sub_max_x},{sub_max_y}'
            }

            urls[i][j] = f'{layer.server_url}?{urlencode(sub_params)}'

    return urls

def geoserver_retrieve_image(layer: Layer, target_crs: str, horizontal_subdivisions: int=1, vertical_subdivisions: int=1) -> Image:
    """
    Retrieve a WMS layer image from GeoServer.
    If horizontal_subdivisions and vertical_subdivisions are provided, the image is retrieved in multiple requests and stitched together.

    Args:
        layer (Layer): The layer to retrieve the image for.
        target_crs (str): Target coordinate reference system (e.g. 'EPSG:3031')
        horizontal_subdivisions (int): Number of horizontal subdivisions.
        vertical_subdivisions (int): Number of vertical subdivisions.

    Returns:
        Image: The retrieved layer image.
    """
    projected_bounds = get_projected_layer_bounds(layer, target_crs)
    ( width, height, _, _, _ ) = get_image_details_from_projected_layer_bounds(*projected_bounds)

    image =  Image.new('RGBA', (width, height))

    urls = subdivide_requests(layer, target_crs, horizontal_subdivisions, vertical_subdivisions)

    for i, h_urls in enumerate(urls):
        for j, url in enumerate(h_urls):
            try:
                response = urlopen(url)
            except HTTPError as e:
                raise RuntimeError(f"URL {url} returned an error response") from e

            try:
                sub_image = Image.open(response)
            except UnidentifiedImageError as e:
                raise RuntimeError(f"Response from URL {url} could not be converted to an image") from e

            sub_image = sub_image.convert('RGBA')

            image.paste(
                sub_image,
                (
                    (width // horizontal_subdivisions) * i,
                    height - (height // vertical_subdivisions) * j - sub_image.height
                ),
                sub_image
            )
    return image

def wms_layer_image(layer: Layer, target_crs: str, horizontal_subdivisions: int, vertical_subdivisions: int) -> Image:
    """
    Retrieve a WMS layer image, optionally subdividing the request.

    Args:
        layer (Layer): The layer to retrieve the image for.
        target_crs (str): Target coordinate reference system (e.g. 'EPSG:3031')
        horizontal_subdivisions (int): Number of horizontal subdivisions.
        vertical_subdivisions (int): Number of vertical subdivisions.

    Returns:
        Image: The retrieved layer image.
    """
    if horizontal_subdivisions or vertical_subdivisions:
        try:
            return geoserver_retrieve_image(layer, target_crs, horizontal_subdivisions, vertical_subdivisions)
        except RuntimeError as e:
            raise RuntimeError(f"Could not retrieve image in {horizontal_subdivisions}x{vertical_subdivisions} subdivisions") from e
    else:
        try:
            return geoserver_retrieve_image(layer, target_crs)
        except RuntimeError as e:
            try:
                return geoserver_retrieve_image(layer, target_crs, 40, 40)
            except RuntimeError as e:
                raise RuntimeError("Could not retrieve image in single request or in 40x40 subdivisions") from e

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
    except (KeyError, IndexError):
        opacity1 = 255
    try:
        opacity2 = unique_value_info2['symbol']['color'][3]
    except (KeyError, IndexError):
        opacity2 = 255

    if style1 == 'esriSFSSolid' and style2 != 'esriSFSSolid':
        return -1
    elif style1 != 'esriSFSSolid' and style2 == 'esriSFSSolid':
        return 1

    return opacity1 - opacity2

def mapserver_vector_layer_image(layer: Layer, target_crs: str) -> Image:
    drawing_info = layer.server_info()['drawingInfo']
    opacity = drawing_info_to_opacity(drawing_info)
    renderer_type = drawing_info['renderer']['type']
    ( min_x, min_y, max_x, max_y ) = get_projected_layer_bounds(layer, target_crs)
    ( _, _, _, _, aspect_ratio ) = get_image_details_from_projected_layer_bounds(min_x, min_y, max_x, max_y)

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
                extent=[min_x, min_y, max_x, max_y],
                **geoplot_args
            )
        elif plot_type == 'line':
            gdf['geometry'] = gdf['geometry'].map(geometry_to_polygon)
            ax = geoplot.polyplot(
                gdf,
                ax=ax,
                extent=[min_x, min_y, max_x, max_y],
                **geoplot_args
            )
        elif plot_type == 'point':
            ax = geoplot.pointplot(
                gdf,
                ax=ax,
                extent=[min_x, min_y, max_x, max_y],
                **geoplot_args
            )

            if symbol.get('imageData'):
                for _, row in gdf.iterrows():
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
                    extent=[min_x, min_y, max_x, max_y],
                    **geoplot_args
                )
            elif plot_type == 'line':
                filtered_gdf['geometry'] = filtered_gdf['geometry'].map(geometry_to_polygon)
                ax = geoplot.polyplot(
                    filtered_gdf,
                    ax=ax,
                    extent=[min_x, min_y, max_x, max_y],
                    **geoplot_args
                )
            elif plot_type == 'point':
                ax = geoplot.pointplot(
                    filtered_gdf,
                    ax=ax,
                    extent=[min_x, min_y, max_x, max_y],
                    **geoplot_args
                )

                if symbol.get('imageData'):
                    for _, row in filtered_gdf.iterrows():
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
        fig.set_size_inches(fig_width, fig_width / aspect_ratio)
        plt.savefig(
            bytes_io,
            format='png',
            transparent=True
        )
        bytes_io.seek(0)
        image = Image.open(bytes_io).copy()
        return image

def featureserver_vector_layer_image(layer: Layer, target_crs) -> Image:
    return mapserver_vector_layer_image(layer, target_crs) # FeatureServer case seemingly the same as MapServer case (for now)

def generate_layer_preview(layer: Layer, target_crs: str, horizontal_subdivisions: int, vertical_subdivisions: int) -> None:
    """
    Generate a layer preview image for the given layer and save it to storage.
    """
    filepath = f'layer_previews/{layer.id}.png'
    projected_layer_bounds = get_projected_layer_bounds(layer, target_crs)
    ( width, height, _, _, _ ) = get_image_details_from_projected_layer_bounds(*projected_layer_bounds)

    layer_image = None
    if re.search(r'^(.+?)/services/(.+?)/MapServer/(?!WMSServer).+$', layer.server_url):
        layer_image = mapserver_vector_layer_image(layer, target_crs)
    elif re.search(r'^(.+?)/services/(.+?)/FeatureServer/(?!WMSServer).+$', layer.server_url):
        layer_image = featureserver_vector_layer_image(layer, target_crs)
    else:
        layer_image = wms_layer_image(layer, target_crs, horizontal_subdivisions, vertical_subdivisions)

    layer_image = layer_image.resize((width, height))
    projected_layer_bounds = get_projected_layer_bounds(layer, target_crs)
    cropped_basemap = get_basemap_cropped_basemap_image(*projected_layer_bounds, target_crs)
    cropped_basemap.paste(layer_image, mask=layer_image)

    default_storage.delete(filepath)
    cropped_basemap.save(default_storage.path(filepath), format='PNG')

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
        parser.add_argument(
            '--target_crs',
            help='Target CRS for the layer preview image (e.g. EPSG:3031 for Antarctica layers)'
        )

    def handle(self, *args, **options):
        layer_id = int(options['layer_id']) if options['layer_id'] is not None else None
        skip_existing = options['skip_existing'].lower() in ['t', 'true'] if options['skip_existing'] is not None else False
        horizontal_subdivisions = int(options['horizontal_subdivisions']) if options['horizontal_subdivisions'] is not None else None
        vertical_subdivisions = int(options['vertical_subdivisions']) if options['vertical_subdivisions'] is not None else None
        target_crs = options['target_crs'] if options['target_crs'] is not None else 'EPSG:4326'
        errors = []

        if layer_id is not None:
            layer = Layer.objects.get(id=layer_id)
            filepath = f'layer_previews/{layer.id}.png'

            if not skip_existing or not default_storage.exists(filepath):
                try:
                    generate_layer_preview(layer, target_crs, horizontal_subdivisions, vertical_subdivisions)
                except Exception as e: # pylint: disable=broad-except
                    logging.error("Error processing layer %s", layer.id, exc_info=e)
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
                        generate_layer_preview(layer, target_crs, horizontal_subdivisions, vertical_subdivisions)
                    except Exception as e: # pylint: disable=broad-except
                        logging.error("Error processing layer %s", layer.id, exc_info=e)
                        errors.append({'layer': layer, 'e': e})

        if errors:
            logging.warning(
                "Failed to retrieve the following layers: \n%s",
                '\n'.join([f" • {error['layer'].name} ({error['layer'].id})" for error in errors])
            )

            if not layer_id:
                email_generate_layer_preview_summary(errors)

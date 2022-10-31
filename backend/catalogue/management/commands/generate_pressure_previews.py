from django.core.management.base import BaseCommand
from django.core.files.storage import default_storage
from django.core.files import File
from django.conf import settings
from urllib.request import urlopen
from urllib.parse import urlencode
from PIL import Image
from io import BytesIO
import logging

from catalogue.models import KeyedLayer, RegionReport

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

def generate_region_pressure_previews(region_report):
    bbox = {
        'north': region_report.maxy,
        'south': region_report.miny,
        'east':  region_report.maxx,
        'west':  region_report.minx
    }

    x_delta = (bbox['east'] - bbox['west'] + 360) % 360
    x_delta = x_delta if x_delta != 0 else 360
    y_delta = bbox['north'] - bbox['south']
    aspect_ratio = x_delta / y_delta
    width = 386
    height = round(width / aspect_ratio)

    cropped_basemap = basemap_bbox(**bbox).resize((width, height))

    with BytesIO() as bytes_io:
        filepath = f'pressure_previews/{region_report.network}' + (f' - {region_report.park}' if region_report.park else '') + '.png'
        cropped_basemap.save(bytes_io, 'PNG')
        default_storage.delete(filepath)
        default_storage.save(filepath, File(bytes_io, ''))

class Command(BaseCommand):
    def add_arguments(self, parser):
        pass

    def handle(self, *args, **options):
        boundary_layer = KeyedLayer.objects.get(keyword='data-report-minimap-panel1-boundary').layer
        for region_report in RegionReport.objects.all():
            generate_region_pressure_previews(region_report, boundary_layer)


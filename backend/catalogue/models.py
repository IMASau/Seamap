# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.

from django.core.validators import MinValueValidator, RegexValidator
from django.db import models
from six import python_2_unicode_compatible
from uuid import uuid4
from datetime import datetime
import requests


@python_2_unicode_compatible
class Category(models.Model):
    "Category for semantic grouping in the UI; eg bathymetry or habitat"
    name = models.CharField(max_length = 200)
    display_name = models.CharField(max_length = 200, null=True)
    sort_key = models.CharField(max_length=10, null=True, blank=True)

    def __str__(self):
        return self.name


@python_2_unicode_compatible
class DataClassification(models.Model):
    """Category for data-type grouping; eg SST, chlorophyll, etc (relevant
    for third-party layers)"""
    name = models.CharField(max_length = 200)
    sort_key = models.CharField(max_length=10, null=True, blank=True)

    def __str__(self):
        return self.name


@python_2_unicode_compatible
class ServerType(models.Model):
    name = models.CharField(max_length = 200)

    def __str__(self):
        return self.name


@python_2_unicode_compatible
class Organisation(models.Model):
    name = models.CharField(max_length = 200)
    logo = models.CharField(max_length = 50, blank=True, null=True)
    sort_key = models.CharField(max_length=10, null=True, blank=True)

    def __str__(self):
        return self.name


# Habitat descriptors; ie, how we map from a habitat key to a colour
# and (optionally) a title:

colour_validator = RegexValidator(regex=r'^#[0-9a-fA-F]{6,6}$',
                                  message="Colour must be in hexadecimal format")

@python_2_unicode_compatible
class HabitatDescriptor(models.Model):
    name = models.CharField(max_length=200, unique=True)
    colour = models.CharField(max_length=7, validators=[colour_validator])
    title = models.CharField(max_length=200, blank=True, null=True)

    def __str__(self):
        return self.name


CRS_CHOICES = [
    ('EPSG:4326', 'EPSG:4326'),
    ('EPSG:3857', 'EPSG:3857'),
    ('EPSG:3112', 'EPSG:3112'),
]


@python_2_unicode_compatible
class Layer(models.Model):
    name = models.CharField(max_length = 200)
    server_url = models.URLField(max_length = 200)
    legend_url = models.URLField(max_length = 250, null=True, blank=True)
    layer_name = models.CharField(max_length = 200)
    detail_layer = models.CharField(max_length = 200, blank=True, null=True)
    table_name = models.CharField(max_length = 200, blank=True, null=True)
    category = models.ForeignKey(Category, on_delete=models.PROTECT)
    data_classification = models.ForeignKey(DataClassification, blank=True, null=True, on_delete=models.PROTECT)
    organisation = models.ForeignKey(Organisation, blank=True, null=True, on_delete=models.PROTECT)
    # Bounding box; store as four separate fields
    minx = models.DecimalField(max_digits=20, decimal_places=17)
    miny = models.DecimalField(max_digits=20, decimal_places=17)
    maxx = models.DecimalField(max_digits=20, decimal_places=17)
    maxy = models.DecimalField(max_digits=20, decimal_places=17)
    metadata_url = models.URLField(max_length = 250)
    server_type = models.ForeignKey(ServerType, on_delete=models.PROTECT)
    sort_key = models.CharField(max_length=10, null=True, blank=True)
    info_format_type = models.IntegerField()
    keywords = models.CharField(max_length = 400, null=True, blank=True)
    style = models.CharField(max_length=200, null=True, blank=True)
    layer_type = models.CharField(max_length=10)
    tooltip = models.TextField(null=True, blank=True)
    metadata_summary = models.TextField(null=True, blank=True)
    crs = models.CharField(max_length=10, choices=CRS_CHOICES, default='EPSG:3112')

    def __str__(self):
        return self.name

    def geojson(self, out_fields: str=None):
        url = f"{self.server_url}/query"
        params = {
            'where':     '1=1',
            'outFields': out_fields,
            'f':         'geojson'
        }

        r = requests.get(url=url, params=params, verify=False)

        return r.json()

    def server_info(self):
        url = self.server_url
        params = { 'f': 'json' }

        r = requests.get(url=url, params=params, verify=False)

        return r.json()

    def cql_property_values(self, cql_properties: list) -> dict:
        params = {
            'service': 'WFS',
            'version': '2.0.0',
            'request': 'GetFeature',
            'typeNames': self.layer_name,
            'outputFormat': 'application/json',
            'propertyName': f"({','.join(cql_properties)})",
        }
        r = requests.get(url=self.server_url, params=params, verify=False)
        data = r.json()

        features_cql_properties = [
            {
                cql_property: feature["properties"][cql_property]
                for cql_property in cql_properties
            }
            for feature in data["features"]
        ]

        value_combinations = [
            dict(y) for y in set(
                tuple(x.items())
                for x in features_cql_properties
            )
        ]

        values = [
            {
                'cql_property': cql_property,
                'values': sorted(set(
                    value_combination[cql_property]
                    for value_combination in value_combinations
                ))
            }
            for cql_property in cql_properties
        ]
        
        return {
            'values': values,
            'value_combinations': value_combinations
        }
        
    def bounds(self):
        return {
            'north': float(self.maxy),
            'south': float(self.miny),
            'east':  float(self.maxx),
            'west':  float(self.minx)
        }

@python_2_unicode_compatible
class BaseLayerGroup(models.Model):
    name = models.CharField(max_length = 200)
    sort_key = models.CharField(max_length=10, null=True, blank=True)

    def __str__(self):
        return self.name


@python_2_unicode_compatible
class BaseLayer(models.Model):
    name = models.CharField(max_length = 200, unique=True)
    server_url = models.URLField(max_length = 200)
    attribution = models.CharField(max_length = 200)
    sort_key = models.CharField(max_length=10, null=True, blank=True)
    layer_group = models.ForeignKey(BaseLayerGroup, blank=True, null=True, on_delete=models.PROTECT, db_column='layer_group')
    layer_type = models.CharField(max_length=10)

    def __str__(self):
        return self.name


@python_2_unicode_compatible
class SaveState(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid4, editable=False)
    hashstate = models.CharField(max_length = 8000)
    description = models.TextField(blank=True, null=True)
    time_created = models.DateTimeField(default=datetime.now())

    def __str__(self):
        return self.description or '{id} ({time_created})'.format(
            id=self.id,
            time_created=self.time_created.strftime('%Y/%m/%d, %H:%M:%S')
        )

@python_2_unicode_compatible
class KeyedLayer(models.Model):
    keyword = models.CharField(max_length = 200)
    layer = models.ForeignKey(Layer, on_delete=models.PROTECT)
    description = models.TextField(null=True, blank=True)
    sort_key = models.CharField(max_length=10, null=True, blank=True)

    def __str__(self):
        return self.keyword

@python_2_unicode_compatible
class RichLayer(models.Model):
    layer = models.ForeignKey(Layer, on_delete=models.PROTECT)
    tab_label = models.CharField(max_length=255)
    slider_label = models.CharField(max_length=255)
    icon = models.CharField(max_length=255)
    tooltip = models.CharField(max_length=255)

    def __str__(self):
        return str(self.layer)

@python_2_unicode_compatible
class RichLayerAlternateView(models.Model):
    richlayer = models.ForeignKey(RichLayer, on_delete=models.PROTECT, related_name='alternate_views')
    layer = models.ForeignKey(Layer, on_delete=models.PROTECT)
    sort_key = models.CharField(max_length=10, null=True, blank=True)

@python_2_unicode_compatible
class RichLayerTimeline(models.Model):
    richlayer = models.ForeignKey(RichLayer, on_delete=models.PROTECT, related_name='timeline')
    layer = models.ForeignKey(Layer, on_delete=models.PROTECT)
    value = models.FloatField(null=False)
    label = models.CharField(max_length=255)

DATA_TYPE_CHOICES = [
    ('string', 'string'),
    ('number', 'number'),
]

CONTROLLER_TYPE_CHOICES = [
    ('slider', 'slider'),
    ('dropdown', 'dropdown'),
    ('multi-dropdown', 'multi-dropdown'),
]

class EmptyStringToNoneField(models.CharField):
    def get_prep_value(self, value):
        if value == '':
            return None
        return value

@python_2_unicode_compatible
class RichLayerControl(models.Model):
    richlayer = models.ForeignKey(RichLayer, on_delete=models.PROTECT, related_name='controls')
    cql_property = models.CharField(max_length=255)
    label = models.CharField(max_length=255)
    data_type = models.CharField(max_length=255, choices=DATA_TYPE_CHOICES)
    controller_type = models.CharField(max_length=255, choices=CONTROLLER_TYPE_CHOICES)
    icon = EmptyStringToNoneField(max_length=255, null=True, blank=True)
    tooltip = EmptyStringToNoneField(max_length=255, null=True, blank=True)
    default_value = EmptyStringToNoneField(max_length=255, null=True, blank=True)

@python_2_unicode_compatible
class RegionReport(models.Model):
    network = models.CharField(max_length=254, null=False, blank=False)
    park = EmptyStringToNoneField(max_length=254, null=True, blank=True)
    habitat_state = models.FloatField(default=0)
    bathymetry_state = models.FloatField(default=0)
    habitat_observations_state = models.FloatField(default=0)
    state_summary = models.TextField()
    slug = models.CharField(max_length=255, null=False, blank=False)
    minx = models.FloatField("Max X", null=False)
    miny = models.FloatField("Max Y", null=False)
    maxx = models.FloatField("Min X", null=False)
    maxy = models.FloatField("Min Y", null=False)

    def __str__(self):
        return self.network + (f' > {self.park}' if self.park else '')


@python_2_unicode_compatible
class Pressure(models.Model):
    region_report = models.ForeignKey(RegionReport, on_delete=models.CASCADE)
    layer = models.ForeignKey(Layer, on_delete=models.PROTECT)
    category = models.CharField(max_length=200, null=False, blank=False)

    def __str__(self):
        return f'{self.region_report}: {self.layer}'


@python_2_unicode_compatible
class DynamicPill(models.Model):
    text = models.CharField(max_length=255)
    icon = EmptyStringToNoneField(max_length=255, null=True, blank=True)
    tooltip = EmptyStringToNoneField(max_length=255, null=True, blank=True)
    url = models.URLField(max_length=255)
    layers = models.ManyToManyField(Layer)
    # consider extracting control into a separate model, and adding as a one-to-one field?
    region_control_cql_property = models.CharField(max_length=255)
    region_control_label = models.CharField(max_length=255)
    region_control_data_type = models.CharField(max_length=255, choices=DATA_TYPE_CHOICES)
    region_control_controller_type = models.CharField(max_length=255, choices=CONTROLLER_TYPE_CHOICES)
    region_control_icon = EmptyStringToNoneField(max_length=255, null=True, blank=True)
    region_control_tooltip = EmptyStringToNoneField(max_length=255, null=True, blank=True)
    region_control_default_value = EmptyStringToNoneField(max_length=255, null=True, blank=True)

    def __str__(self):
        return self.text

# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.

from django.core.validators import MinValueValidator, RegexValidator
from django.db import models
from six import python_2_unicode_compatible
from uuid import uuid4
from datetime import datetime


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

    def __str__(self):
        return self.name


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
    hashstate = models.CharField(max_length = 5000)
    description = models.TextField(blank=True, null=True)
    time_created = models.DateTimeField(default=datetime.now())

    def __str__(self):
        return '{id} ({time_created})'.format(
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
class NationalLayerTimeline(models.Model):
    layer = models.ForeignKey(Layer, on_delete=models.PROTECT)
    year = models.IntegerField()

    def __str__(self):
        return f'{self.layer} ({self.year})'

from __future__ import unicode_literals
import re

from django.core.validators import MinValueValidator, RegexValidator
from django.db import models
from django.utils.encoding import python_2_unicode_compatible


@python_2_unicode_compatible
class Category(models.Model):
    "Category for semantic grouping in the UI; eg bathymetry or habitat"
    name = models.CharField(max_length = 200)

    def __str__(self):
        return self.name


@python_2_unicode_compatible
class DataClassification(models.Model):
    """Category for data-type grouping; eg SST, chlorophyll, etc (relevant
    for third-party layers)"""
    name = models.CharField(max_length = 200)

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


# We have groups of layers, for the purposes of automatic layer
# switching logic.  A group is a collection of overlapping layers, and
# layers have a priority within that group that determines in what
# order they're displayed, and (using a cutoff) which are displayed or
# not in auto-mode.  A group is also either a detail resolution (most
# of them; the local area layers), or not (probably only one; the
# national group).


@python_2_unicode_compatible
class LayerGroup(models.Model):
    name = models.CharField(max_length = 200)
    detail_resolution = models.NullBooleanField(default=True)

    def __str__(self):
        return self.name


@python_2_unicode_compatible
class Layer(models.Model):
    name = models.CharField(max_length = 200)
    server_url = models.URLField(max_length = 200)
    layer_name = models.CharField(max_length = 200)
    category = models.ForeignKey(Category)
    data_classification = models.ForeignKey(DataClassification, blank=True, null=True)
    organisation = models.ForeignKey(Organisation, blank=True, null=True)
    # Bounding box; store as four separate fields
    minx = models.DecimalField(max_digits=20, decimal_places=17)
    miny = models.DecimalField(max_digits=20, decimal_places=17)
    maxx = models.DecimalField(max_digits=20, decimal_places=17)
    maxy = models.DecimalField(max_digits=20, decimal_places=17)
    metadata_url = models.URLField(max_length = 200)
    description = models.CharField(max_length = 500)
    server_type = models.ForeignKey(ServerType)
    sort_key = models.CharField(max_length=10, null=True, blank=True)

    def __str__(self):
        return self.name


@python_2_unicode_compatible
class LayerGroupPriority(models.Model):
    priority = models.IntegerField(default=1, validators=[MinValueValidator(1)])
    group = models.ForeignKey(LayerGroup, related_name='layerpriorities')
    layer = models.ForeignKey(Layer, related_name='grouppriorities')

    def __str__(self):
        return '{}:[{} / {}]'.format(self.priority,
                                     str(self.layer),
                                     str(self.group))

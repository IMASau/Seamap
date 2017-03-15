from __future__ import unicode_literals
import re

from django.core.validators import RegexValidator
from django.db import models
from django.utils.encoding import python_2_unicode_compatible


@python_2_unicode_compatible
class Category(models.Model):
    name = models.CharField(max_length = 200)

    def __str__(self):
        return self.name


@python_2_unicode_compatible
class ServerType(models.Model):
    name = models.CharField(max_length = 200)

    def __str__(self):
        return self.name


comma_separated_float_list_re = re.compile(r'^[-\.\d,]+\Z')  # no, not very sophisticated
validate_comma_separated_float_list = RegexValidator(
    comma_separated_float_list_re,
    'Must be a comma-separated list of floating-point numbers',
    'invalid'
)

@python_2_unicode_compatible
class Layer(models.Model):
    name = models.CharField(max_length = 200)
    server_url = models.URLField(max_length = 200)
    layer_name = models.CharField(max_length = 200)
    category = models.ForeignKey(Category)
    bounding_box = models.CharField(validators=[validate_comma_separated_float_list], max_length = 200)
    metadata_url = models.URLField(max_length = 200)
    description = models.CharField(max_length = 500)
    zoom_info = models.CharField(validators=[validate_comma_separated_float_list], max_length = 200)
    detail_resolution = models.BooleanField()
    server_type = models.ForeignKey(ServerType)
    legend_url = models.URLField(max_length=200)
    date_start = models.DateField()
    date_end = models.DateField()

    def __str__(self):
        return self.name

# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models
import re
import django.core.validators


class Migration(migrations.Migration):

    dependencies = [
        ('catalogue', '0003_layer_detail_resolution'),
    ]

    operations = [
        migrations.AlterField(
            model_name='layer',
            name='bounding_box',
            field=models.CharField(max_length=200, validators=[django.core.validators.RegexValidator(re.compile('^[-\\.\\d,]+\\Z'), 'Must be a comma-separated list of floating-point numbers', 'invalid')]),
        ),
        migrations.AlterField(
            model_name='layer',
            name='date_end',
            field=models.DateField(null=True, blank=True),
        ),
        migrations.AlterField(
            model_name='layer',
            name='date_start',
            field=models.DateField(null=True, blank=True),
        ),
        migrations.AlterField(
            model_name='layer',
            name='zoom_info',
            field=models.CharField(max_length=200, validators=[django.core.validators.RegexValidator(re.compile('^[-\\.\\d,]+\\Z'), 'Must be a comma-separated list of floating-point numbers', 'invalid')]),
        ),
    ]

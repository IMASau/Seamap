# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
# -*- coding: utf-8 -*-


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

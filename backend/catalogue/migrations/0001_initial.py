# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations
import django.core.validators
import re


class Migration(migrations.Migration):

    dependencies = [
    ]

    operations = [
        migrations.CreateModel(
            name='Category',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('name', models.CharField(max_length=200)),
            ],
        ),
        migrations.CreateModel(
            name='Layer',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('name', models.CharField(max_length=200)),
                ('url', models.URLField()),
                ('bounding_box', models.CharField(max_length=200, validators=[django.core.validators.RegexValidator(re.compile('^[\\d,]+$'), 'Enter only digits separated by commas.', 'invalid')])),
                ('metadata_url', models.URLField()),
                ('description', models.CharField(max_length=500)),
                ('zoom_info', models.CharField(max_length=200, validators=[django.core.validators.RegexValidator(re.compile('^[\\d,]+$'), 'Enter only digits separated by commas.', 'invalid')])),
                ('legend_url', models.URLField()),
                ('date_start', models.DateField()),
                ('date_end', models.DateField()),
                ('category', models.ForeignKey(to='catalogue.Category')),
            ],
        ),
        migrations.CreateModel(
            name='ServerType',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('name', models.CharField(max_length=200)),
            ],
        ),
        migrations.AddField(
            model_name='layer',
            name='server_type',
            field=models.ForeignKey(to='catalogue.ServerType'),
        ),
    ]

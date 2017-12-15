# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models
import django.core.validators


class Migration(migrations.Migration):

    dependencies = [
        ('catalogue', '0014_auto_20170622_1153'),
    ]

    operations = [
        migrations.CreateModel(
            name='HabitatDescriptor',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('name', models.CharField(unique=True, max_length=200)),
                ('colour', models.CharField(max_length=7, validators=[django.core.validators.RegexValidator(regex='^#[0-9a-fA-F]{6,6}$', message='Colour must be in hexadecimal format')])),
                ('title', models.CharField(max_length=200, null=True, blank=True)),
            ],
        ),
    ]

# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('catalogue', '0011_layer_layer_group'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='layer',
            name='date_end',
        ),
        migrations.RemoveField(
            model_name='layer',
            name='date_start',
        ),
    ]

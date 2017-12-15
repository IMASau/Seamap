# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('catalogue', '0017_layer_detail_layer'),
    ]

    operations = [
        migrations.AddField(
            model_name='organisation',
            name='logo',
            field=models.CharField(max_length=50, null=True, blank=True),
        ),
    ]

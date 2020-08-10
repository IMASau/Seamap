# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
# -*- coding: utf-8 -*-


from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('catalogue', '0006_auto_20170417_1548'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='layer',
            name='legend_url',
        ),
        migrations.RemoveField(
            model_name='layer',
            name='zoom_info',
        ),
    ]

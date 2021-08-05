# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
# -*- coding: utf-8 -*-


from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('catalogue', '0008_auto_20170428_1815'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='layer',
            name='bounding_box',
        ),
        migrations.AddField(
            model_name='layer',
            name='maxx',
            field=models.DecimalField(default=0, max_digits=20, decimal_places=17),
            preserve_default=False,
        ),
        migrations.AddField(
            model_name='layer',
            name='maxy',
            field=models.DecimalField(default=0, max_digits=20, decimal_places=17),
            preserve_default=False,
        ),
        migrations.AddField(
            model_name='layer',
            name='minx',
            field=models.DecimalField(default=0, max_digits=20, decimal_places=17),
            preserve_default=False,
        ),
        migrations.AddField(
            model_name='layer',
            name='miny',
            field=models.DecimalField(default=0, max_digits=20, decimal_places=17),
            preserve_default=False,
        ),
    ]

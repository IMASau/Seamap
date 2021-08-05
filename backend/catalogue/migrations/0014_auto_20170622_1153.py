# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
# -*- coding: utf-8 -*-


from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('catalogue', '0013_auto_20170615_1156'),
    ]

    operations = [
        migrations.AlterField(
            model_name='layergroup',
            name='detail_resolution',
            field=models.NullBooleanField(default=True),
        ),
        migrations.AlterField(
            model_name='layergrouppriority',
            name='group',
            field=models.ForeignKey(related_name='layerpriorities', to='catalogue.LayerGroup', on_delete=models.PROTECT),
        ),
        migrations.AlterField(
            model_name='layergrouppriority',
            name='layer',
            field=models.ForeignKey(related_name='grouppriorities', to='catalogue.Layer', on_delete=models.PROTECT),
        ),
    ]

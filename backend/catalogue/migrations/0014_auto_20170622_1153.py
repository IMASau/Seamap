# -*- coding: utf-8 -*-
from __future__ import unicode_literals

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
            field=models.ForeignKey(related_name='layerpriorities', to='catalogue.LayerGroup'),
        ),
        migrations.AlterField(
            model_name='layergrouppriority',
            name='layer',
            field=models.ForeignKey(related_name='grouppriorities', to='catalogue.Layer'),
        ),
    ]

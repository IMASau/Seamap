# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('catalogue', '0010_auto_20170525_1504'),
    ]

    operations = [
        migrations.AddField(
            model_name='layer',
            name='layer_group',
            field=models.ForeignKey(default=1, to='catalogue.LayerGroup'),
        ),
    ]

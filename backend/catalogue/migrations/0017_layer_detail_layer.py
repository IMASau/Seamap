# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('catalogue', '0016_layer_sort_key'),
    ]

    operations = [
        migrations.AddField(
            model_name='layer',
            name='detail_layer',
            field=models.CharField(max_length=200, null=True, blank=True),
        ),
    ]

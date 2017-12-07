# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('catalogue', '0020_layer_table_name'),
    ]

    operations = [
        migrations.AlterField(
            model_name='layer',
            name='table_name',
            field=models.CharField(max_length=200),
        ),
    ]

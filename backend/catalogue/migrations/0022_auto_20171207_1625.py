# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('catalogue', '0021_auto_20171207_1508'),
    ]

    operations = [
        migrations.AlterField(
            model_name='layer',
            name='table_name',
            field=models.CharField(max_length=200, null=True, blank=True),
        ),
    ]

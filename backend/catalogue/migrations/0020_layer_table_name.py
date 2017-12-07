# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('catalogue', '0019_auto_20171107_1701'),
    ]

    operations = [
        migrations.AddField(
            model_name='layer',
            name='table_name',
            field=models.CharField(default='', max_length=200),
        ),
    ]

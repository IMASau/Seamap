# -*- coding: utf-8 -*-
from __future__ import unicode_literals

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

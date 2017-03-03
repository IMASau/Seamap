# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('catalogue', '0002_auto_20170217_1206'),
    ]

    operations = [
        migrations.AddField(
            model_name='layer',
            name='detail_resolution',
            field=models.BooleanField(default=True),
            preserve_default=False,
        ),
    ]

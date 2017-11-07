# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('catalogue', '0018_organisation_logo'),
    ]

    operations = [
        migrations.AddField(
            model_name='dataclassification',
            name='sort_key',
            field=models.CharField(max_length=10, null=True, blank=True),
        ),
        migrations.AddField(
            model_name='organisation',
            name='sort_key',
            field=models.CharField(max_length=10, null=True, blank=True),
        ),
    ]

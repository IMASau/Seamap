# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('catalogue', '0007_auto_20170419_1156'),
    ]

    operations = [
        migrations.AlterField(
            model_name='layer',
            name='data_classification',
            field=models.ForeignKey(blank=True, to='catalogue.DataClassification', null=True),
        ),
        migrations.AlterField(
            model_name='layer',
            name='organisation',
            field=models.ForeignKey(blank=True, to='catalogue.Organisation', null=True),
        ),
    ]

# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models
import django.core.validators


class Migration(migrations.Migration):

    dependencies = [
        ('catalogue', '0012_auto_20170602_1608'),
    ]

    operations = [
        migrations.CreateModel(
            name='LayerGroupPriority',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('priority', models.IntegerField(default=1, validators=[django.core.validators.MinValueValidator(1)])),
            ],
        ),
        migrations.RemoveField(
            model_name='layer',
            name='detail_resolution',
        ),
        migrations.RemoveField(
            model_name='layer',
            name='layer_group',
        ),
        migrations.RemoveField(
            model_name='layer',
            name='layer_priority',
        ),
        migrations.AddField(
            model_name='layergroup',
            name='detail_resolution',
            field=models.BooleanField(default=True),
        ),
        migrations.AddField(
            model_name='layergrouppriority',
            name='group',
            field=models.ForeignKey(to='catalogue.LayerGroup'),
        ),
        migrations.AddField(
            model_name='layergrouppriority',
            name='layer',
            field=models.ForeignKey(to='catalogue.Layer'),
        ),
    ]

# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
# -*- coding: utf-8 -*-


from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('catalogue', '0009_auto_20170517_1153'),
    ]

    operations = [
        migrations.CreateModel(
            name='LayerGroup',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('name', models.CharField(max_length=200)),
            ],
        ),
        migrations.AddField(
            model_name='layer',
            name='layer_priority',
            field=models.IntegerField(default=1),
        ),
    ]

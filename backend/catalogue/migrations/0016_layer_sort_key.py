# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
# -*- coding: utf-8 -*-


from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('catalogue', '0015_habitatdescriptor'),
    ]

    operations = [
        migrations.AddField(
            model_name='layer',
            name='sort_key',
            field=models.CharField(max_length=10, null=True, blank=True),
        ),
    ]

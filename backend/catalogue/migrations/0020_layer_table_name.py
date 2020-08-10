# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
# -*- coding: utf-8 -*-


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

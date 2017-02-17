# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import migrations, models
import re
import django.core.validators


class Migration(migrations.Migration):

    dependencies = [
        ('catalogue', '0001_initial'),
    ]

    operations = [
        migrations.RenameField(
            model_name='layer',
            old_name='url',
            new_name='server_url',
        ),
        migrations.AddField(
            model_name='layer',
            name='layer_name',
            field=models.CharField(default='Layer Name', max_length=200),
            preserve_default=False,
        ),
        migrations.AlterField(
            model_name='layer',
            name='bounding_box',
            field=models.CharField(max_length=200, validators=[django.core.validators.RegexValidator(re.compile('^[\\d,]+\\Z'), 'Enter only digits separated by commas.', 'invalid')]),
        ),
        migrations.AlterField(
            model_name='layer',
            name='zoom_info',
            field=models.CharField(max_length=200, validators=[django.core.validators.RegexValidator(re.compile('^[\\d,]+\\Z'), 'Enter only digits separated by commas.', 'invalid')]),
        ),
    ]

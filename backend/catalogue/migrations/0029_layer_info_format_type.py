# Generated by Django 2.2.16 on 2022-05-04 04:50

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('catalogue', '0028_auto_20220127_0009'),
    ]

    operations = [
        migrations.AddField(
            model_name='layer',
            name='info_format_type',
            field=models.IntegerField(default=1),
            preserve_default=False,
        ),
    ]

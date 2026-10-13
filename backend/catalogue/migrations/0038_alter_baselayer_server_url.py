from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('catalogue', '0037_remove_layer_metadata_content_and_more'),
    ]

    operations = [
        migrations.AlterField(
            model_name='baselayer',
            name='server_url',
            field=models.CharField(max_length=200),
        ),
    ]

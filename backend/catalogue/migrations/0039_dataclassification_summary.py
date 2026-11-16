from django.db import migrations
import tinymce.models


class Migration(migrations.Migration):

    dependencies = [
        ('catalogue', '0038_alter_baselayer_server_url'),
    ]

    operations = [
        migrations.AddField(
            model_name='dataclassification',
            name='summary',
            field=tinymce.models.HTMLField(blank=True, null=True),
        ),
    ]

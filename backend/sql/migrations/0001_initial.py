# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import os.path

from django.db import migrations
from django.conf import settings


class RunScript(migrations.RunSQL):
    def __init__(self, filename, **kwargs):
        path = os.path.join(settings.SQL_ROOT, filename)
        assert os.path.exists(path)
        sql = open(path).read()
        sql = sql.replace("\nGO\n","\n--GO\n")
        super(RunScript, self).__init__(sql, **kwargs)


class Migration(migrations.Migration):

    dependencies = [
        ('auth', '__latest__'),
    ]

    operations = [

        RunScript('Types/HabitatTableType.sql', reverse_sql='DROP TYPE HabitatTableType'),
        RunScript('Functions/simplify_geoms.sql', reverse_sql='DROP FUNCTION simplify_geoms'),
        RunScript('Functions/path_intersections.sql', reverse_sql='DROP FUNCTION path_intersections'),

    ]

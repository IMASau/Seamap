# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.

from django.db import models

import tinymce.models
from six import python_2_unicode_compatible


class EmptyHTMLToNoneField(tinymce.models.HTMLField):
    def get_prep_value(self, value):
        if value == '':
            return None
        return value


@python_2_unicode_compatible
class SiteConfiguration(models.Model):
    name = models.CharField(max_length=255)
    outage_message = EmptyHTMLToNoneField(blank=True, null=True)

    def __str__(self):
        return self.name

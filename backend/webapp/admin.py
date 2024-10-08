# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.

from django.contrib import admin
from webapp import models


class SiteConfigurationAdmin(admin.ModelAdmin):
    readonly_fields = ('last_modified',)
admin.site.register(models.SiteConfiguration, SiteConfigurationAdmin)

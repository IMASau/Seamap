# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.

from webapp import models
from rest_framework import serializers


class SiteConfigurationSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.SiteConfiguration
        exclude = ('id', 'name', )

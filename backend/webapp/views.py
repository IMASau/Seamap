# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.

from webapp import models, serializers
from rest_framework.generics import RetrieveAPIView


class SiteConfigurationView(RetrieveAPIView):
    serializer_class = serializers.SiteConfigurationSerializer

    def get_object(self):
        name = self.request.query_params['name']
        if not name:
            raise ValueError('name parameter is required')
        return models.SiteConfiguration.objects.get(name=name)

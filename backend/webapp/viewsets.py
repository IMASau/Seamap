# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
from . import models
from django.views.decorators.cache import cache_page
from rest_framework.decorators import action, api_view
from rest_framework.response import Response
from rest_framework.request import Request


@action(methods=['GET'], detail=False)
@cache_page(60 * 15)
@api_view()
def site_configuration(request: Request):
    return Response(
        {site_configuration.keyword: site_configuration.value
        for site_configuration
        in models.SiteConfiguration.objects.all()}
    )

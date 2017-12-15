# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
from django.conf import settings
from django.conf.urls import include, url
from django.contrib import admin
from rest_framework.routers import DefaultRouter

from catalogue.viewsets import ClassificationViewset, DescriptorViewset, LayerViewset, GroupViewset, GroupPriorityViewset, OrganisationViewset
from habitat.viewsets import regions, subset, transect


router = DefaultRouter()
router.register(r'classifications', ClassificationViewset)
router.register(r'layers', LayerViewset)
router.register(r'groups', GroupViewset)
router.register(r'organisations', OrganisationViewset)
router.register(r'priorities', GroupPriorityViewset)
router.register(r'descriptors', DescriptorViewset)

urlpatterns = [
    url(r'^api/habitat/transect', transect),
    url(r'^api/habitat/regions', regions, name='habitat-regions'),
    url(r'^api/habitat/subset', subset),
    url(r'^api/', include(router.urls)),
]

if settings.DEBUG:
    urlpatterns += [
        url(r'^admin/', admin.site.urls),
        url(r'^auth/', include('rest_framework.urls', namespace='rest_framework')),
    ]

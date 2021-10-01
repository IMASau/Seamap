# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
from django.conf import settings
from django.contrib import admin
from django.urls import include, re_path
from rest_framework.routers import DefaultRouter

from catalogue.viewsets import ClassificationViewset, DescriptorViewset, BaseLayerViewset, LayerViewset, GroupViewset, GroupPriorityViewset, OrganisationViewset
from habitat.viewsets import regions, subset, transect


router = DefaultRouter()
router.register(r'classifications', ClassificationViewset)
router.register(r'baselayers', BaseLayerViewset)
router.register(r'layers', LayerViewset)
router.register(r'groups', GroupViewset)
router.register(r'organisations', OrganisationViewset)
router.register(r'priorities', GroupPriorityViewset)
router.register(r'descriptors', DescriptorViewset)

urlpatterns = [
    re_path(r'^api/habitat/transect', transect),
    re_path(r'^api/habitat/regions', regions, name='habitat-regions'),
    re_path(r'^api/habitat/subset', subset),
    re_path(r'^api/', include(router.urls)),
]

if settings.DEBUG:
    urlpatterns += [
        re_path(r'^admin/', admin.site.urls),
        re_path(r'^auth/', include('rest_framework.urls', namespace='rest_framework')),
    ]

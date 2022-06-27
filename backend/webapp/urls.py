# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
from django.conf import settings
from django.contrib import admin
from django.urls import include, re_path
from rest_framework.routers import DefaultRouter

import catalogue.viewsets as viewsets

from catalogue.views import SaveStateView
from habitat.viewsets import regions, subset, transect, networks, parks, zones, zones_iucn, habitat_statistics, bathymetry_statistics


router = DefaultRouter()
router.register(r'classifications', viewsets.ClassificationViewset)
router.register(r'baselayers', viewsets.BaseLayerViewset)
router.register(r'layers', viewsets.LayerViewset)
router.register(r'groups', viewsets.GroupViewset)
router.register(r'organisations', viewsets.OrganisationViewset)
router.register(r'priorities', viewsets.GroupPriorityViewset)
router.register(r'descriptors', viewsets.DescriptorViewset)
router.register(r'baselayergroups', viewsets.BaseLayerGroupViewset)
router.register(r'categories', viewsets.CategoryViewset)

urlpatterns = [
    re_path(r'^api/habitat/transect', transect),
    re_path(r'^api/habitat/regions', regions, name='habitat-regions'),
    re_path(r'^api/habitat/subset', subset),
    re_path(r'^api/habitat/networks/?$', networks),
    re_path(r'^api/habitat/parks/?$', parks),
    re_path(r'^api/habitat/zones/?$', zones),
    re_path(r'^api/habitat/zonesiucn/?$', zones_iucn),
    re_path(r'^api/habitat/habitatstatistics', habitat_statistics),
    re_path(r'^api/habitat/bathymetrystatistics', bathymetry_statistics),
    re_path(r'^api/savestates', SaveStateView.as_view()),
    re_path(r'^api/', include(router.urls)),
]

if settings.DEBUG:
    urlpatterns += [
        re_path(r'^admin/', admin.site.urls),
        re_path(r'^auth/', include('rest_framework.urls', namespace='rest_framework')),
    ]

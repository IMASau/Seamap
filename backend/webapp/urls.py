# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
from django.conf import settings
from django.contrib import admin
from django.urls import include, re_path
from rest_framework.routers import DefaultRouter
from django.conf import settings
from django.conf.urls.static import static

import catalogue.viewsets as viewsets

from catalogue.views import SaveStateView
from habitat.viewsets import regions, subset, transect, amp_boundaries, imcra_boundaries, meow_boundaries, habitat_statistics, bathymetry_statistics, habitat_observations


router = DefaultRouter()
router.register(r'classifications', viewsets.ClassificationViewset)
router.register(r'baselayers', viewsets.BaseLayerViewset)
router.register(r'layers', viewsets.LayerViewset)
router.register(r'groups', viewsets.GroupViewset)
router.register(r'organisations', viewsets.OrganisationViewset)
router.register(r'descriptors', viewsets.DescriptorViewset)
router.register(r'baselayergroups', viewsets.BaseLayerGroupViewset)
router.register(r'categories', viewsets.CategoryViewset)
router.register(r'keyedlayers', viewsets.KeyedLayerViewset)

urlpatterns = [
    re_path(r'^api/habitat/transect', transect),
    re_path(r'^api/habitat/regions', regions, name='habitat-regions'),
    re_path(r'^api/habitat/subset', subset),
    re_path(r'^api/habitat/ampboundaries', amp_boundaries),
    re_path(r'^api/habitat/imcraboundaries', imcra_boundaries),
    re_path(r'^api/habitat/meowboundaries', meow_boundaries),
    re_path(r'^api/habitat/habitatstatistics', habitat_statistics),
    re_path(r'^api/habitat/bathymetrystatistics', bathymetry_statistics),
    re_path(r'^api/habitat/habitatobservations', habitat_observations),
    re_path(r'^api/savestates', SaveStateView.as_view()),
    re_path(r'^api/', include(router.urls)),
] + static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)

if settings.DEBUG:
    urlpatterns += [
        re_path(r'^admin/', admin.site.urls),
        re_path(r'^auth/', include('rest_framework.urls', namespace='rest_framework')),
    ]

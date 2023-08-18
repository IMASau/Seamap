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
import habitat.viewsets as habitat_viewsets

router = DefaultRouter()
router.register(r'classifications', viewsets.ClassificationViewset)
router.register(r'baselayers', viewsets.BaseLayerViewset)
router.register(r'layers', viewsets.LayerViewset)
router.register(r'organisations', viewsets.OrganisationViewset)
router.register(r'descriptors', viewsets.DescriptorViewset)
router.register(r'baselayergroups', viewsets.BaseLayerGroupViewset)
router.register(r'categories', viewsets.CategoryViewset)
router.register(r'keyedlayers', viewsets.KeyedLayerViewset)
router.register(r'richlayeralternateview', viewsets.RichLayerAlternateViewViewset)
router.register(r'richlayertimeline', viewsets.RichLayerTimelineViewset)
router.register(r'regionreports', viewsets.RegionReportViewset)

urlpatterns = [
    re_path(r'^api/habitat/transect', habitat_viewsets.transect),
    re_path(r'^api/habitat/regions', habitat_viewsets.regions, name='habitat-regions'),
    re_path(r'^api/habitat/subset', habitat_viewsets.subset),
    re_path(r'^api/habitat/ampboundaries', habitat_viewsets.amp_boundaries),
    re_path(r'^api/habitat/imcraboundaries', habitat_viewsets.imcra_boundaries),
    re_path(r'^api/habitat/meowboundaries', habitat_viewsets.meow_boundaries),
    re_path(r'^api/habitat/habitatstatistics', habitat_viewsets.habitat_statistics),
    re_path(r'^api/habitat/bathymetrystatistics', habitat_viewsets.bathymetry_statistics),
    re_path(r'^api/habitat/habitatobservations', habitat_viewsets.habitat_observations),
    re_path(r'^api/habitat/regionreportdata', habitat_viewsets.region_report_data),
    re_path(r'^api/habitat/datainregion', habitat_viewsets.data_in_region),
    re_path(r'^api/savestates', SaveStateView.as_view()),
    re_path(r'^api/', include(router.urls)),
    re_path(r'^admin/', admin.site.urls),
    re_path(r'^auth/', include('rest_framework.urls', namespace='rest_framework')),
] + static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)

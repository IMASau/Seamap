from django.urls import re_path

from nhat import viewsets

urlpatterns = [
    re_path(r'^layerlegend/(?P<layer_id>[^/.]+)', viewsets.layer_legend, name='layer_legend'),
]

rf_routes = [
    (r'layers', viewsets.LayerViewset, 'nhatlayer'), # basename=nhatlayer
    (r'cmipphases', viewsets.CmipPhaseViewset),
    (r'scientificmodels', viewsets.ScientificModelViewset),
    (r'scenarios', viewsets.ScenarioViewset),
    (r'seasons', viewsets.SeasonViewset),
]

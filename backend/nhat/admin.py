from django.contrib import admin
from django.db.models.query import QuerySet
from django.http import HttpRequest

import nhat.models as models

admin.site.register(models.HazardLayer)
admin.site.register(models.CmipPhase)
admin.site.register(models.ScientificModel)
admin.site.register(models.Scenario)
admin.site.register(models.Season)

@admin.register(models.HazardLayerDataset)
class HazardLayerDatasetAdmin(admin.ModelAdmin):
    list_display = (
        'hazard_layer',
        'cmip_phase',
        'scientific_model',
        'scenario',
        'season',
        'is_historical',
    )

    def get_queryset(self, request: HttpRequest) -> QuerySet:
        queryset = super().get_queryset(request)
        return queryset.prefetch_related(
            'hazard_layer',
            'cmip_phase',
            'scientific_model',
            'scenario',
            'season',
        )

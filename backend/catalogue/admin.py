# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
from django.contrib import admin
import catalogue.models as models

admin.site.register(models.Category)
admin.site.register(models.DataClassification)
admin.site.register(models.ServerType)
admin.site.register(models.Organisation)
admin.site.register(models.HabitatDescriptor)
admin.site.register(models.BaseLayerGroup)
admin.site.register(models.BaseLayer)

class LayerAdmin(admin.ModelAdmin):
    search_fields = ('name',)
admin.site.register(models.Layer, LayerAdmin)

class SaveStateAdmin(admin.ModelAdmin):
    readonly_fields = ('time_created',)
admin.site.register(models.SaveState, SaveStateAdmin)

class KeyedLayerAdmin(admin.ModelAdmin):
    autocomplete_fields = ('layer',)
admin.site.register(models.KeyedLayer, KeyedLayerAdmin)

class RichLayerAlternateViewInline(admin.TabularInline):
    autocomplete_fields = ('layer',)
    model = models.RichLayerAlternateView
    fk_name = 'richlayer'
    extra = 0

class RichLayerTimelineInline(admin.TabularInline):
    autocomplete_fields = ('layer',)
    model = models.RichLayerTimeline
    fk_name = 'richlayer'
    extra = 0

class RichLayerAdmin(admin.ModelAdmin):
    inlines = (RichLayerAlternateViewInline, RichLayerTimelineInline,)
    autocomplete_fields = ('layer',)
admin.site.register(models.RichLayer, RichLayerAdmin)

class PressureAdminInline(admin.TabularInline):
    autocomplete_fields = ('layer',)
    model = models.Pressure
    extra = 0

class RegionReportAdmin(admin.ModelAdmin):
    inlines = (PressureAdminInline,)
    fields = (
        'network',
        'park',
        ('habitat_state','bathymetry_state','habitat_observations_state',),
        'state_summary',
        'slug',
        ('minx','maxx','miny','maxy',),
    )
admin.site.register(models.RegionReport, RegionReportAdmin)

class SquidleAnnotationsDataAdmin(admin.ModelAdmin):
    readonly_fields = ('last_modified',)
    ordering = ('-last_modified',)
admin.site.register(models.SquidleAnnotationsData, SquidleAnnotationsDataAdmin)

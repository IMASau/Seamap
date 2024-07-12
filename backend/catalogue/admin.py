# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
from django import forms
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

class RichLayerControlInline(admin.TabularInline):
    model = models.RichLayerControl
    fk_name = 'richlayer'
    extra = 0

class RichLayerAdmin(admin.ModelAdmin):
    inlines = (RichLayerAlternateViewInline, RichLayerTimelineInline, RichLayerControlInline,)
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

class DynamicPillForm(forms.ModelForm):
    class Meta:
        labels = {
            'region_control_cql_property': 'CQL Property',
            'region_control_label': 'Label',
            'region_control_data_type': 'Data Type',
            'region_control_controller_type': 'Controller Type',
            'region_control_icon': 'Icon',
            'region_control_tooltip': 'Tooltip',
            'region_control_default_value': 'Default Value',
        }

class DynamicPillAdmin(admin.ModelAdmin):
    form = DynamicPillForm
    autocomplete_fields = ('layers',)
    fieldsets = [
        (
            None,
            {
                'fields': ['text','icon','tooltip', 'layers']
            }
        ),
        (
            "Region Control",
            {
                'fields': ['region_control_cql_property','region_control_label','region_control_data_type','region_control_controller_type','region_control_icon','region_control_tooltip','region_control_default_value']
            },
        ),
    ]
admin.site.register(models.DynamicPill, DynamicPillAdmin)

# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
from django.contrib import admin
from .models import Category, DataClassification, Organisation, ServerType, Layer, LayerGroup, LayerGroupPriority, HabitatDescriptor, BaseLayerGroup

admin.site.register(Category)
admin.site.register(DataClassification)
admin.site.register(Organisation)
admin.site.register(ServerType)
admin.site.register(HabitatDescriptor)
admin.site.register(Layer)
admin.site.register(LayerGroup)
admin.site.register(LayerGroupPriority)
admin.site.register(BaseLayerGroup)

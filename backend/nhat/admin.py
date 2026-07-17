from django.contrib import admin, messages
from django.core.management import call_command
from django.db.models.query import QuerySet
from django.http import HttpRequest, HttpResponse
from django.shortcuts import redirect
from django.urls import path, reverse, URLPattern
from typing import cast, Optional
from django.contrib.messages import get_messages

import nhat.models as models
from nhat.management.commands.refresh_hazard_layers import Command as RefreshHazardLayersCommand

admin.site.register(models.CmipPhase)
admin.site.register(models.ScientificModel)
admin.site.register(models.Scenario)
admin.site.register(models.Season)


@admin.register(models.HazardLayer)
class HazardLayerAdmin(admin.ModelAdmin):
    def get_urls(self) -> list[URLPattern]:
        urls = cast(list[URLPattern], super().get_urls()) # silences mypy strict type checking
        my_urls = [
            path(
                "refresh-hazard-layers/",
                self.admin_site.admin_view(self.refresh_hazard_layers),
                name="hazardlayer_refreshfromserver",
            )
        ]
        return my_urls + urls # Yes, this is how Django suggests doing this: https://docs.djangoproject.com/en/6.0/ref/contrib/admin/#django.contrib.admin.ModelAdmin.get_urls

    def changelist_view(self, request: HttpRequest, extra_context: Optional[dict[str, str]] = None) -> HttpResponse:
        extra_context = extra_context or {}
        extra_context["refresh_url"] = reverse("admin:hazardlayer_refreshfromserver")

        return super().changelist_view(
            request,
            extra_context=extra_context,
        )

    def refresh_hazard_layers(self, request: HttpRequest) -> HttpResponse:
        command = RefreshHazardLayersCommand()
        call_command(command, server_url="https://thredds-nhat-dev.its.utas.edu.au/thredds/")

        if command.is_failure:
            self.message_user(
                request,
                "Refresh complete. Some hazard layers were unable to be loaded.",
                messages.WARNING,
            )
        else:
            self.message_user(
                request,
                "Refresh complete.",
                messages.SUCCESS,
            )

        return redirect("../")


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

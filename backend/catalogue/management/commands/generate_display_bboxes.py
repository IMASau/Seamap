# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.

"""
Management command to generate layer display bounding boxes.

Uses BBOX_SERIALIZER to transform the bounding box coordinates.
"""
import importlib
import logging
from typing import Callable, Optional
from django.conf import settings
from django.core.management.base import BaseCommand
from django.db import models
from django.db.models import Q

# pylint: disable=line-too-long
# pylint: disable=missing-class-docstring


class LayerDisplay(models.Model):
    minx = models.DecimalField(max_digits=20, decimal_places=17)
    miny = models.DecimalField(max_digits=20, decimal_places=17)
    maxx = models.DecimalField(max_digits=20, decimal_places=17)
    maxy = models.DecimalField(max_digits=20, decimal_places=17)
    display_maxx = models.DecimalField(max_digits=20, decimal_places=17)
    display_maxy = models.DecimalField(max_digits=20, decimal_places=17)
    display_minx = models.DecimalField(max_digits=20, decimal_places=17)
    display_miny = models.DecimalField(max_digits=20, decimal_places=17)
    # There are other columns - used in catalogue.models.Layer - but we don't care
    # about them


def get_bbox_fn():
    "Function for transforming bounding box coordinates"
    if bbox_serializer := getattr(settings, "BBOX_SERIALIZER", None):
        bbox_module_name, bbox_fn_name = bbox_serializer.rsplit('.', 1)
        try:
            bbox_module = importlib.import_module(bbox_module_name)
            return getattr(bbox_module, bbox_fn_name)
        except ModuleNotFoundError:
            logging.error("Error trying to import bbox function %s", bbox_serializer, exc_info=True)
            raise
    else:
        logging.error("BBOX_SERIALIZER is not definied in settings")
        raise RuntimeError("BBOX_SERIALIZER is not definied in settings")


def layer_queryset(layer_id: Optional[int], skip_existing: bool):
    "Queryset for which layers we'll be updating the display bbox coordinates."
    layers = LayerDisplay.objects.all()

    # Filter by layer_id if provided
    if layer_id is not None:
        layers = layers.filter(id=layer_id)

    # Filter out objects that already have display values if skip_existing is True
    if skip_existing:
        layers = layers.filter(
            Q(display_maxx__isnull=True) or
            Q(display_maxy__isnull=True) or
            Q(display_minx__isnull=True) or
            Q(display_miny__isnull=True)
        )

    return layers


def generate_display_bbox(layer: LayerDisplay, bbox_fn: Callable):
    """
    Generate the display bbox coordinates for a given LayerDisplay object, and
    update the object with the new values.
    """
    bbox = bbox_fn(layer)
    layer.display_maxx = bbox["east"]
    layer.display_maxy = bbox["north"]
    layer.display_minx = bbox["west"]
    layer.display_miny = bbox["south"]


class Command(BaseCommand):
    def add_arguments(self, parser):
        parser.add_argument(
            '--layer_id',
            help='Specify a layer ID for the layer preview image you want to generate',
            type=int,
        )
        parser.add_argument(
            '--skip_existing',
            help='If false, skips generating images for layers where images already exist',
            type=bool,
            default=False,
        )

    def handle(self, *args, **options):
        layer_id = options['layer_id']
        skip_existing = options['skip_existing']
        bbox_fn = get_bbox_fn()

        # Update the bounding box values for each layer, then save in bulk update
        layers = layer_queryset(layer_id, skip_existing)
        for layer in layers:
            generate_display_bbox(layer, bbox_fn)
        LayerDisplay.objects.bulk_update(layers, ['display_maxx', 'display_maxy', 'display_minx', 'display_miny'])

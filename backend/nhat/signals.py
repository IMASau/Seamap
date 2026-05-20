from django.core.cache import cache
from django.db.models.signals import post_save, post_delete
from django.dispatch import receiver
from catalogue.models import Layer
from .models import HazardLayer

@receiver([post_save, post_delete], sender=HazardLayer)
@receiver([post_save, post_delete], sender=Layer)
def clear_nhatlayer_list_cache(**kwargs):
    cache.delete("nhatlayer_list")

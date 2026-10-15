from django.core.cache import cache
from django.db.models.signals import post_save, post_delete
from django.dispatch import receiver
from .models import Layer

@receiver([post_save, post_delete], sender=Layer)
def clear_layer_list_cache(**kwargs):
    cache.delete("layer_list")

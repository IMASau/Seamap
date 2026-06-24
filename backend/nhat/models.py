"""Database models for the NHAT app."""
from django.db import models
from catalogue.models import Layer


COLOR_PALETTE_CHOICES = [
    ('default', 'default'),
    ('div-BrBG', 'div-BrBG'),
    ('div-BuRd', 'div-BuRd'),
    ('div-BuRd2', 'div-BuRd2'),
    ('div-PRGn', 'div-PRGn'),
    ('div-PiYG', 'div-PiYG'),
    ('div-PuOr', 'div-PuOr'),
    ('div-RdBu', 'div-RdBu'),
    ('div-RdGy', 'div-RdGy'),
    ('div-RdYlBu', 'div-RdYlBu'),
    ('div-RdYlGn', 'div-RdYlGn'),
    ('div-Spectral', 'div-Spectral'),
    ('psu-inferno', 'psu-inferno'),
    ('psu-magma', 'psu-magma'),
    ('psu-plasma', 'psu-plasma'),
    ('psu-viridis', 'psu-viridis'),
    ('seq-BkBu', 'seq-BkBu'),
    ('seq-BkGn', 'seq-BkGn'),
    ('seq-BkRd', 'seq-BkRd'),
    ('seq-BkYl', 'seq-BkYl'),
    ('seq-BlueHeat', 'seq-BlueHeat'),
    ('seq-Blues', 'seq-Blues'),
    ('seq-BuGn', 'seq-BuGn'),
    ('seq-BuPu', 'seq-BuPu'),
    ('seq-BuYl', 'seq-BuYl'),
    ('seq-GnBu', 'seq-GnBu'),
    ('seq-Greens', 'seq-Greens'),
    ('seq-Greys', 'seq-Greys'),
    ('seq-GreysRev', 'seq-GreysRev'),
    ('seq-Heat', 'seq-Heat'),
    ('seq-OrRd', 'seq-OrRd'),
    ('seq-Oranges', 'seq-Oranges'),
    ('seq-PuBu', 'seq-PuBu'),
    ('seq-PuBuGn', 'seq-PuBuGn'),
    ('seq-PuRd', 'seq-PuRd'),
    ('seq-Purples', 'seq-Purples'),
    ('seq-RdPu', 'seq-RdPu'),
    ('seq-Reds', 'seq-Reds'),
    ('seq-YlGn', 'seq-YlGn'),
    ('seq-YlGnBu', 'seq-YlGnBu'),
    ('seq-YlOrBr', 'seq-YlOrBr'),
    ('seq-YlOrRd', 'seq-YlOrRd'),
    ('seq-cubeYF', 'seq-cubeYF'),
    ('x-Ncview', 'x-Ncview'),
    ('x-Occam', 'x-Occam'),
    ('x-Rainbow', 'x-Rainbow'),
    ('x-Sst', 'x-Sst'),
]

class HazardLayer(models.Model):
    """Hazard layer info added to a standard Thredds server layer."""
    layer = models.OneToOneField(
        Layer,
        on_delete=models.CASCADE,
        primary_key=True,
    )
    color_scale_range_min = models.IntegerField(default=0)
    color_scale_range_max = models.IntegerField(default=100)
    above_max_color = models.CharField(max_length=8, default='0x000000')
    below_min_color = models.CharField(max_length=8, default='0x000000')
    color_palette = models.CharField(max_length=50, choices=COLOR_PALETTE_CHOICES, default='default')

    def __str__(self):
        return self.layer.name

class CmipPhase(models.Model):
    """
    CMIP (Coupled Model Intercomparison Project) phase that organizes models and
    scenarios for analyzing hazard data.
    """
    name = models.SlugField(unique=True)
    display_name = models.CharField(max_length=50, unique=True)
    sort_key = models.CharField(max_length=10, null=True, blank=True)
    
    def __str__(self):
        return self.display_name
    
    class Meta:
        verbose_name = "CMIP phase"

class ScientificModel(models.Model):
    """Scientific model to analyze the hazard data."""
    name = models.SlugField(unique=True)
    display_name = models.CharField(max_length=50, unique=True)
    sort_key = models.CharField(max_length=10, null=True, blank=True)

    def __str__(self):
        return self.display_name

class Scenario(models.Model):
    """Scenario to analyze the hazard data."""
    name = models.SlugField(unique=True)
    display_name = models.CharField(max_length=50, unique=True)
    sort_key = models.CharField(max_length=10, null=True, blank=True)

    def __str__(self):
        return self.display_name

class Season(models.Model):
    """Seasonal data to analyze the hazard data."""
    name = models.SlugField(unique=True)
    display_name = models.CharField(max_length=50, unique=True)
    sort_key = models.CharField(max_length=10, null=True, blank=True)

    def __str__(self):
        return self.display_name

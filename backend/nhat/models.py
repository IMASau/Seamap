"""Database models for the NHAT app."""
from django.db import models
from django.db.models import Q
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
        on_delete=models.PROTECT,
        primary_key=True,
    )
    name = models.SlugField(
        unique=True,
        help_text="""
            Unique slug used for the NetCDF file name to associate it with this hazard layer.
        """,
    )
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
    name = models.SlugField(
        unique=True,
        help_text="""
            Unique slug used for the NetCDF file name to associate it with this CMIP phase.
        """,
    )
    display_name = models.CharField(
        max_length=50,
        unique=True,
        help_text="Name displayed for this CMIP phase in the interactive map.",
    )
    sort_key = models.CharField(
        max_length=10,
        null=True,
        blank=True,
        help_text="""
            Determines the order CMIP phases are shown in the app. Sorted
            alphabetically/numerically, with blanks last. First CMIP phase in order is the
            default selection when the app is first loaded.
        """
    )

    def __str__(self):
        return self.display_name

    class Meta:
        verbose_name = "CMIP phase"

DATA_CATEGORY_CHOICES = [
    ('ensemble_statistic', 'ensemble_statistic'),
    ('model', 'model'),
]

class ScientificModel(models.Model):
    """Scientific model to analyze the hazard data."""
    name = models.SlugField(
        unique=True,
        help_text="""
            Unique slug used for the NetCDF file name to associate it with this model.
        """,
    )
    display_name = models.CharField(
        max_length=50,
        unique=True,
        help_text="Name displayed for this model in the interactive map.",
    )
    sort_key = models.CharField(
        max_length=10,
        null=True,
        blank=True,
        help_text="""
            Determines the order scientific models are shown in the app. Sorted
            alphabetically/numerically, with blanks last. First model in order is the
            default selection when the app is first loaded.
        """,
    )
    data_category = models.CharField(max_length=50, choices=DATA_CATEGORY_CHOICES)

    def __str__(self):
        return self.display_name

class Scenario(models.Model):
    """Scenario to analyze the hazard data."""
    name = models.SlugField(
        unique=True,
        help_text="""
            Unique slug used for the NetCDF file name to associate it with this scenario.
        """,
    )
    display_name = models.CharField(
        max_length=50,
        unique=True,
        help_text="Name displayed for this scenario in the interactive map.",
    )
    sort_key = models.CharField(
        max_length=10,
        null=True,
        blank=True,
        help_text="""
            Determines the order scenarios are shown in the app. Sorted
            alphabetically/numerically, with blanks last. First scenario in order is the
            default selection when the app is first loaded.
        """,
    )

    def __str__(self):
        return self.display_name

class Season(models.Model):
    """Seasonal data to analyze the hazard data."""
    name = models.SlugField(
        unique=True,
        help_text="""
            Unique slug used for the NetCDF file name to associate it with this season.
        """,
    )
    display_name = models.CharField(
        max_length=50,
        unique=True,
        help_text="Name displayed for this season in the interactive map.",
    )
    sort_key = models.CharField(
        max_length=10,
        null=True,
        blank=True,
        help_text="""
            Determines the order seasons are shown in the app. Sorted alphabetically/numerically, with blanks last. First CMIP phase in order is the default selection when the app is first loaded.
        """,
    )

    def __str__(self):
        return self.display_name

class HazardLayerDataset(models.Model):
    """Dataset associated with a hazard layer."""
    hazard_layer = models.ForeignKey(
        HazardLayer,
        related_name="datasets",
        on_delete=models.PROTECT,
    )
    cmip_phase = models.ForeignKey(
        CmipPhase,
        related_name="datasets",
        on_delete=models.PROTECT,
    )
    scientific_model = models.ForeignKey(
        ScientificModel,
        related_name="datasets",
        on_delete=models.PROTECT,
    )
    scenario = models.ForeignKey(
        Scenario,
        related_name="datasets",
        null=True,
        blank=True,
        on_delete=models.PROTECT
    )
    season = models.ForeignKey(
        Season,
        related_name="datasets",
        on_delete=models.PROTECT,
    )
    color_scale_range_min = models.FloatField(default=0)
    color_scale_range_max = models.FloatField(default=100)
    is_historical = models.BooleanField(default=False)

    def __str__(self):
        scenario_display = "Historical" if self.is_historical else self.scenario
        return f"{self.hazard_layer} Dataset ({self.cmip_phase} / {self.scientific_model} / {scenario_display} / {self.season})"

    class Meta:
        constraints = [
            models.CheckConstraint(
                check=(
                    (Q(is_historical=True) & Q(scenario__isnull=True)) |
                    (Q(is_historical=False) & Q(scenario__isnull=False))
                ),
                name="historical_requires_null_scenario",
            ),
            models.UniqueConstraint(
                fields=[
                    "hazard_layer",
                    "cmip_phase",
                    "scientific_model",
                    "season",
                ],
                condition=Q(is_historical=True),
                name="unique_historical_dataset",
            ),
            models.UniqueConstraint(
                fields=[
                    "hazard_layer",
                    "cmip_phase",
                    "scientific_model",
                    "scenario",
                    "season",
                ],
                condition=Q(is_historical=False),
                name="unique_projected_dataset",
            ),
        ]

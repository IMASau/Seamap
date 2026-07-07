"""Database models for the NHAT app."""
from django.db import models
from django.db.models import Q
from catalogue.models import Layer


COLOR_PALETTE_CHOICES = [
    ('default', 'default'),
    ('default-inv', 'default-inv'),
    ('div-BrBG', 'div-BrBG'),
    ('div-BrBG-inv', 'div-BrBG-inv'),
    ('div-BuRd', 'div-BuRd'),
    ('div-BuRd-inv', 'div-BuRd-inv'),
    ('div-BuRd2', 'div-BuRd2'),
    ('div-BuRd2-inv', 'div-BuRd2-inv'),
    ('div-PRGn', 'div-PRGn'),
    ('div-PRGn-inv', 'div-PRGn-inv'),
    ('div-PiYG', 'div-PiYG'),
    ('div-PiYG-inv', 'div-PiYG-inv'),
    ('div-PuOr', 'div-PuOr'),
    ('div-PuOr-inv', 'div-PuOr-inv'),
    ('div-RdBu', 'div-RdBu'),
    ('div-RdBu-inv', 'div-RdBu-inv'),
    ('div-RdGy', 'div-RdGy'),
    ('div-RdGy-inv', 'div-RdGy-inv'),
    ('div-RdYlBu', 'div-RdYlBu'),
    ('div-RdYlBu-inv', 'div-RdYlBu-inv'),
    ('div-RdYlGn', 'div-RdYlGn'),
    ('div-RdYlGn-inv', 'div-RdYlGn-inv'),
    ('div-Spectral', 'div-Spectral'),
    ('div-Spectral-inv', 'div-Spectral-inv'),
    ('psu-inferno', 'psu-inferno'),
    ('psu-inferno-inv', 'psu-inferno-inv'),
    ('psu-magma', 'psu-magma'),
    ('psu-magma-inv', 'psu-magma-inv'),
    ('psu-plasma', 'psu-plasma'),
    ('psu-plasma-inv', 'psu-plasma-inv'),
    ('psu-viridis', 'psu-viridis'),
    ('psu-viridis-inv', 'psu-viridis-inv'),
    ('seq-BkBu', 'seq-BkBu'),
    ('seq-BkBu-inv', 'seq-BkBu-inv'),
    ('seq-BkGn', 'seq-BkGn'),
    ('seq-BkGn-inv', 'seq-BkGn-inv'),
    ('seq-BkRd', 'seq-BkRd'),
    ('seq-BkRd-inv', 'seq-BkRd-inv'),
    ('seq-BkYl', 'seq-BkYl'),
    ('seq-BkYl-inv', 'seq-BkYl-inv'),
    ('seq-BlueHeat', 'seq-BlueHeat'),
    ('seq-BlueHeat-inv', 'seq-BlueHeat-inv'),
    ('seq-Blues', 'seq-Blues'),
    ('seq-Blues-inv', 'seq-Blues-inv'),
    ('seq-BuGn', 'seq-BuGn'),
    ('seq-BuGn-inv', 'seq-BuGn-inv'),
    ('seq-BuPu', 'seq-BuPu'),
    ('seq-BuPu-inv', 'seq-BuPu-inv'),
    ('seq-BuYl', 'seq-BuYl'),
    ('seq-BuYl-inv', 'seq-BuYl-inv'),
    ('seq-GnBu', 'seq-GnBu'),
    ('seq-GnBu-inv', 'seq-GnBu-inv'),
    ('seq-Greens', 'seq-Greens'),
    ('seq-Greens-inv', 'seq-Greens-inv'),
    ('seq-Greys', 'seq-Greys'),
    ('seq-Greys-inv', 'seq-Greys-inv'),
    ('seq-GreysRev', 'seq-GreysRev'),
    ('seq-GreysRev-inv', 'seq-GreysRev-inv'),
    ('seq-Heat', 'seq-Heat'),
    ('seq-Heat-inv', 'seq-Heat-inv'),
    ('seq-OrRd', 'seq-OrRd'),
    ('seq-OrRd-inv', 'seq-OrRd-inv'),
    ('seq-Oranges', 'seq-Oranges'),
    ('seq-Oranges-inv', 'seq-Oranges-inv'),
    ('seq-PuBu', 'seq-PuBu'),
    ('seq-PuBu-inv', 'seq-PuBu-inv'),
    ('seq-PuBuGn', 'seq-PuBuGn'),
    ('seq-PuBuGn-inv', 'seq-PuBuGn-inv'),
    ('seq-PuRd', 'seq-PuRd'),
    ('seq-PuRd-inv', 'seq-PuRd-inv'),
    ('seq-Purples', 'seq-Purples'),
    ('seq-Purples-inv', 'seq-Purples-inv'),
    ('seq-RdPu', 'seq-RdPu'),
    ('seq-RdPu-inv', 'seq-RdPu-inv'),
    ('seq-Reds', 'seq-Reds'),
    ('seq-Reds-inv', 'seq-Reds-inv'),
    ('seq-YlGn', 'seq-YlGn'),
    ('seq-YlGn-inv', 'seq-YlGn-inv'),
    ('seq-YlGnBu', 'seq-YlGnBu'),
    ('seq-YlGnBu-inv', 'seq-YlGnBu-inv'),
    ('seq-YlOrBr', 'seq-YlOrBr'),
    ('seq-YlOrBr-inv', 'seq-YlOrBr-inv'),
    ('seq-YlOrRd', 'seq-YlOrRd'),
    ('seq-YlOrRd-inv', 'seq-YlOrRd-inv'),
    ('seq-cubeYF', 'seq-cubeYF'),
    ('seq-cubeYF-inv', 'seq-cubeYF-inv'),
    ('x-Ncview', 'x-Ncview'),
    ('x-Ncview-inv', 'x-Ncview-inv'),
    ('x-Occam', 'x-Occam'),
    ('x-Occam-inv', 'x-Occam-inv'),
    ('x-Rainbow', 'x-Rainbow'),
    ('x-Rainbow-inv', 'x-Rainbow-inv'),
    ('x-Sst', 'x-Sst'),
    ('x-Sst-inv', 'x-Sst-inv'),
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
    human_readable_units = models.CharField(max_length=50, default='units')

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

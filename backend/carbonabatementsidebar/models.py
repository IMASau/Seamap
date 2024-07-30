# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.

from django.db import models
from six import python_2_unicode_compatible


@python_2_unicode_compatible
class AbatementModel(models.Model):
    id = models.IntegerField(db_column='ID', primary_key=True)
    region = models.CharField(max_length=50)
    cp35 = models.FloatField()
    cp50 = models.FloatField()
    cp65 = models.FloatField()
    cp80 = models.FloatField()
    cpmax = models.FloatField()
    dr = models.IntegerField()
    ec = models.IntegerField()
    ac = models.IntegerField()

    def __str__(self):
        return f"{self.id} {self.region}"

    def save(self, **kwargs):
        raise NotImplementedError()

    class Meta:
        abstract = True
        managed = False


@python_2_unicode_compatible
class CarbonAbatementByState(AbatementModel):
    class Meta(AbatementModel.Meta):
        db_table = 'VW_ANALYTICS_CSIRO_BlueCarbon_tCO2_byState'


@python_2_unicode_compatible
class CarbonAbatementBySa2(AbatementModel):
    class Meta(AbatementModel.Meta):
        db_table = 'VW_ANALYTICS_CSIRO_BlueCarbon_tCO2_bySA2'


@python_2_unicode_compatible
class CarbonAbatementByPrimaryCompartment(AbatementModel):
    class Meta(AbatementModel.Meta):
        db_table = 'VW_ANALYTICS_CSIRO_BlueCarbon_tCO2_byPrimaryCompartment'


@python_2_unicode_compatible
class AbatementAreaByState(AbatementModel):
    class Meta(AbatementModel.Meta):
        db_table = 'VW_ANALYTICS_CSIRO_BlueCarbon_CO2area_byState'


@python_2_unicode_compatible
class AbatementAreaBySa2(AbatementModel):
    class Meta(AbatementModel.Meta):
        db_table = 'VW_ANALYTICS_CSIRO_BlueCarbon_CO2area_bySA2'


@python_2_unicode_compatible
class AbatementAreaByPrimaryCompartment(AbatementModel):
    class Meta(AbatementModel.Meta):
        db_table = 'VW_ANALYTICS_CSIRO_BlueCarbon_CO2area_byPrimaryCompartment'

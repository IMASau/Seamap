from django.db import models

class Transect(models.Model):
    name = models.CharField(max_length = 200)
    start_percentage = models.FloatField()
    end_percentage = models.FloatField()
    startx = models.DecimalField(max_digits=10, decimal_places=2)
    starty = models.DecimalField(max_digits=10, decimal_places=2)
    endx = models.DecimalField(max_digits=10, decimal_places=2)
    endy = models.DecimalField(max_digits=10, decimal_places=2)

    def __str__(self):
        return '{}: {} ({},{}) -> ({},{})'.format(self.name, self.percentage, self.startx, self.starty, self.endx, self.endy)

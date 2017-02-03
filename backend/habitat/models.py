from django.db import models

class Transect(models.Model):
    name = models.CharField(max_length = 200)
    startx = models.DecimalField(max_digits=10, decimal_places=5)
    starty = models.DecimalField(max_digits=10, decimal_places=5)
    endx = models.DecimalField(max_digits=10, decimal_places=5)
    endy = models.DecimalField(max_digits=10, decimal_places=5)
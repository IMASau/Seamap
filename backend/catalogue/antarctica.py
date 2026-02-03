# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.

"""
This module contains utilities etc specific to the Antarctic implementation.

In general this codebase attempts to be generic, but specifics of the
polar CRS in particular make this difficult.
"""
import itertools
import numpy
from pyproj import CRS, Transformer


def serialise_bbox(_self, obj) -> dict:
    """Plugin method to implement Antarctic-specific bounding-box
    projection. The issue is that the bounding box is used in the app
    to zoom to extents,etc. In other words the main property of
    interest is the *visual* extent, which is quite different from the
    geographic/4326 bounds -- for example the left and right edge in
    geographic coords are a single line from the pole to bottom of the
    viewport, while the northern geographic edge entirely wraps the
    continent (and the southern edge is actually a point).

    So: in this implementation we generate a series of points along
    each edge -- could probably just use the northern edge -- and
    calculate the min and max from each axis from the complete set of
    points. These construct a new rectangle (roughly) corresponding to
    the viewport extent, which we can then unproject to get the useful
    geographic coords expected by the frontend.
    """
    transformer = Transformer.from_crs("epsg:4326", "epsg:3031", always_xy=True)
    inverter = Transformer.from_crs("epsg:3031", "epsg:4326", always_xy=True)
    crs = CRS("epsg:3031")

    CUTOFF_RATIO = 0.5
    usable_bbox = crs.area_of_use
    # Hack: because we know the CRS goes from 90 to 60, to
    # calculate the area of overlap we can just look at the
    # vertical ratio of segments above and below the northern CRS
    # edge:
    crs_top = usable_bbox.north
    if crs_top >= obj.maxy:
        overlap_ratio = 1
    elif crs_top <= obj.miny:
        overlap_ratio = 0
    else:
        portion_above = float(obj.maxy) - crs_top
        portion_below = crs_top - float(obj.miny)
        overlap_ratio = portion_below / (portion_above + portion_below)
    if overlap_ratio < CUTOFF_RATIO:
        # return unmodified:
        return {"west": obj.minx, "south": obj.miny, "east": obj.maxx, "north": obj.maxy}
    # https://gis.stackexchange.com/a/197336
    p_0 = numpy.array((float(obj.minx), float(obj.miny)))
    p_1 = numpy.array((float(obj.minx), float(obj.maxy)))
    p_2 = numpy.array((float(obj.maxx), float(obj.miny)))
    p_3 = numpy.array((float(obj.maxx), float(obj.maxy)))
    edge_samples = 11
    _transform = lambda p: transformer.transform(p[0], p[1])
    # list of list of points for each edge; note the '+' does
    # element-wise addition for numpy arrays, so this sneakily
    # iterates along each edge:
    edge_pts = [[_transform(x*i + y*(1-i)) for i in numpy.linspace(0,1,edge_samples)]
                for x,y in [(p_0, p_1),
                            (p_1, p_2),
                            (p_2, p_3),
                            (p_3, p_0)]]
    # Flatten out (we just want all points; "edge" isn't a useful
    # concept at this point, we just want the extremes of each
    # axis to construct a new bounding box from):
    edge_pts = list( itertools.chain.from_iterable(edge_pts) )
    xs = [p[0] for p in edge_pts]
    ys = [p[1] for p in edge_pts]
    minx = min(xs)
    maxx = max(xs)
    miny = min(ys)
    maxy = max(ys)

    # now transform back again:
    minx, miny = inverter.transform(minx, miny)
    maxx, maxy = inverter.transform(maxx, maxy)
    return {"west": minx, "south": miny, "east": maxx, "north": maxy}

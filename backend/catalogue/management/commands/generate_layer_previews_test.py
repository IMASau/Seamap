import rasterio
import numpy as np
from rasterio.warp import reproject, Resampling
from rasterio.transform import from_bounds, from_origin


def reproject_and_crop(
    src_path,
    out_path,
    dst_bounds_3031,
    dst_resolution,
    force_src_crs=None,
    force_src_transform=None,
):
    """
    Reproject a raster from EPSG:4326 to EPSG:3031 and crop to target bounds.

    dst_bounds_3031 = (min_x, min_y, max_x, max_y)
    dst_resolution  = pixel size in metres (for EPSG:3031)
    force_src_crs = set this if the source file has no CRS
    force_src_transform = set this if the source file has no geotransform
    """

    with rasterio.open(src_path) as src:

        # -------------------------
        # 1. Fix missing CRS
        # -------------------------
        if src.crs is None:
            if force_src_crs is None:
                raise ValueError(
                    "Source raster has NO CRS. "
                    "Pass force_src_crs='EPSG:4326' or similar."
                )
            print(f"⚠️ Setting missing CRS to {force_src_crs}")
            src_crs = rasterio.crs.CRS.from_string(force_src_crs)
        else:
            src_crs = src.crs

        # -------------------------
        # 2. Fix missing geotransform
        # -------------------------
        if src.transform == rasterio.Affine.identity():
            if force_src_transform is None:
                raise ValueError(
                    "Source raster has NO geotransform. "
                    "You must supply force_src_transform."
                )
            print(f"⚠️ Setting missing transform: {force_src_transform}")
            src_transform = force_src_transform
        else:
            src_transform = src.transform

        # -------------------------
        # 3. Prepare output geometry in 3031
        # -------------------------
        dst_crs = "EPSG:3031"
        min_x, min_y, max_x, max_y = dst_bounds_3031

        width  = int((max_x - min_x) / dst_resolution)
        height = int((max_y - min_y) / dst_resolution)

        dst_transform = from_bounds(min_x, min_y, max_x, max_y, width, height)

        # -------------------------
        # 4. Prepare output profile
        # -------------------------
        profile = src.profile.copy()
        profile.update({
            "crs": dst_crs,
            "transform": dst_transform,
            "width": width,
            "height": height,
        })

        # -------------------------
        # 5. Reproject band-by-band
        # -------------------------
        with rasterio.open(out_path, "w", **profile) as dst:
            for i in range(1, src.count + 1):
                src_array = src.read(i)
                dst_array = np.empty((height, width), dtype=src_array.dtype)

                reproject(
                    src_array,
                    dst_array,
                    src_transform=src_transform,
                    src_crs=src_crs,
                    dst_transform=dst_transform,
                    dst_crs=dst_crs,
                    resampling=Resampling.nearest,
                )

                dst.write(dst_array, i)

    print(f"✅ Done. Wrote: {out_path}")


with rasterio.open("land_shallow_topo_21600.tif") as src:
    transform_4326 = from_bounds(-180, -90, 180, 90,
                                width=src.width, height=src.height)

reproject_and_crop(
    "land_shallow_topo_21600.tif",
    "cropped_3031.tif",
    (-4200000, -4200000, 4200000, 4200000),
    1000,
    force_src_crs="EPSG:4326",
    force_src_transform=transform_4326,
)

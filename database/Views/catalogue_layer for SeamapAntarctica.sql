-- This is code specific to Seamap Antarctica.
-- In Seamap Antarctica we had an issue with transformed bounding boxes loading
-- slowly (ISA-704), and so we elected to abstract over the catalogue_layer table
-- with a brand-new view.
-- This is the code to generate the abstraction.

BEGIN TRANSACTION;

-- 1. Add display bbox columns
ALTER TABLE [dbo].[catalogue_layer]
ADD
    [display_maxx] DECIMAL(8,5) NULL,
    [display_maxy] DECIMAL(8,5) NULL,
    [display_minx] DECIMAL(8,5) NULL,
    [display_miny] DECIMAL(8,5) NULL;
GO

-- 2. Rename original table
EXEC sp_rename 'catalogue_layer', 'catalogue_layerdisplay';
GO

-- 3. Create view to replace original table
CREATE VIEW [dbo].[catalogue_layer] AS
SELECT
    [id],
    [name],
    [server_url],
    [layer_name],
    [metadata_url],
    [server_type_id],
    [category_id],
    [data_classification_id],
    [organisation_id],
    COALESCE([display_maxx], [maxx]) AS [maxx],
    COALESCE([display_maxy], [maxy]) AS [maxy],
    COALESCE([display_minx], [minx]) AS [minx],
    COALESCE([display_miny], [miny]) AS [miny],
    [sort_key],
    [detail_layer],
    [table_name],
    [legend_url],
    [info_format_type],
    [keywords],
    [style],
    [layer_type],
    [tooltip],
    [metadata_summary],
    [crs],
    [regenerate_preview],
    [filter],
    [download_format]
FROM [dbo].[catalogue_layerdisplay];
GO

-- 4. Grant permissions
GRANT SELECT
ON [dbo].[catalogue_layer]
TO IMASSeamap;
GO

COMMIT TRANSACTION;

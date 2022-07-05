-- When an IMCRA boundary has been updated, use this stored procedure to update the
-- hardcoded values in the BOUNDARY_IMCRA_HABITAT and BOUNDARY_IMCRA_BATHYMETRY
-- tables (can also be used to add a new boundary to the tables).

CREATE PROCEDURE Update_IMCRA_BOUNDARY
  @provincial_bioregion  NVARCHAR(255),
  @mesoscale_bioregion   NVARCHAR(255)
AS
BEGIN
  -- Update BOUNDARY_IMCRA_HABITAT
  DELETE FROM [dbo].[BOUNDARY_IMCRA_HABITAT]
  WHERE
    ([Provincial_Bioregion] = @provincial_bioregion OR @provincial_bioregion IS NULL) AND
    ([Mesoscale_Bioregion] = @mesoscale_bioregion OR @mesoscale_bioregion IS NULL);

  INSERT INTO [dbo].[BOUNDARY_IMCRA_HABITAT] ([Provincial_Bioregion], [Mesoscale_Bioregion], [habitat], [geom])
  SELECT
    [boundary].[Provincial_Bioregion],
    [boundary].[Mesoscale_Bioregion],
    [habitat].[CATEGORY] AS [habitat],
    [habitat].[geom]
  FROM [dbo].[VW_BOUNDARY_IMCRA] AS [boundary]
  CROSS APPLY [dbo].habitat_intersections([boundary].[geom]) AS [habitat]
  WHERE
    ([boundary].[Provincial_Bioregion] = @provincial_bioregion OR @provincial_bioregion IS NULL) AND
    ([boundary].[Mesoscale_Bioregion] = @mesoscale_bioregion OR @mesoscale_bioregion IS NULL);

  -- Update BOUNDARY_IMCRA_BATHYMETRY
  DELETE FROM [dbo].[BOUNDARY_IMCRA_BATHYMETRY]
  WHERE
    ([Provincial_Bioregion] = @provincial_bioregion OR @provincial_bioregion IS NULL) AND
    ([Mesoscale_Bioregion] = @mesoscale_bioregion OR @mesoscale_bioregion IS NULL);
  
  INSERT INTO [dbo].[BOUNDARY_IMCRA_BATHYMETRY] ([Provincial_Bioregion], [Mesoscale_Bioregion], [bathymetry_resolution], [bathymetry_rank], [geom])
  SELECT
    [boundary].[Provincial_Bioregion],
    [boundary].[Mesoscale_Bioregion],
    [bathymetry].[RESOLUTION] AS [bathymetry_resolution],
    [bathymetry].[RANK] AS [bathymetry_rank],
    [bathymetry].[geom]
  FROM [dbo].[VW_BOUNDARY_IMCRA] AS [boundary]
  CROSS APPLY [dbo].unique_bathymetry_intersections([boundary].[geom]) AS [bathymetry]
  WHERE
    ([boundary].[Provincial_Bioregion] = @provincial_bioregion OR @provincial_bioregion IS NULL) AND
    ([boundary].[Mesoscale_Bioregion] = @mesoscale_bioregion OR @mesoscale_bioregion IS NULL);
END;

CREATE TABLE [dbo].[squidle_annotations_data] (
  [id]               INT           NOT NULL PRIMARY KEY IDENTITY(1,1),
  [NETNAME]          NVARCHAR(255) NOT NULL,
  [RESNAME]          NVARCHAR(255) NULL,
  [ZONENAME]         NVARCHAR(255) NULL,
  [HIGHLIGHTS]       BIT           NOT NULL,
  [ANNOTATIONS_DATA] NVARCHAR(MAX) NULL
);

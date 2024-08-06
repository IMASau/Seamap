CREATE TABLE [dbo].[webapp_siteconfiguration] (
  [id]             INT           NOT NULL PRIMARY KEY IDENTITY(1,1),
  [name]           NVARCHAR(255) NOT NULL,
  [outage_message] NVARCHAR(MAX) NULL
);

CREATE TABLE [dbo].[webapp_siteconfiguration] (
  [id]             INT           NOT NULL PRIMARY KEY IDENTITY(1,1),
  [keyword]        NVARCHAR(255) NOT NULL,
  [value]          NVARCHAR(MAX) NULL,
  [last_modified]  DATETIME      NOT NULL DEFAULT GETDATE()
);

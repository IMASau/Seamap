CREATE TABLE [dbo].[catalogue_dynamicpill] (
  [id]      INT           NOT NULL PRIMARY KEY IDENTITY(1,1),
  [text]    NVARCHAR(MAX) NOT NULL,
  [icon]    NVARCHAR(MAX) NULL,
  [tooltip] NVARCHAR(MAX) NULL
);

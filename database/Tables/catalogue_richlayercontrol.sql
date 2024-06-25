CREATE TABLE [dbo].[catalogue_richlayercontrol] (
  [id]              INT           NOT NULL PRIMARY KEY IDENTITY(1,1),
  [richlayer_id]    INT           NOT NULL FOREIGN KEY REFERENCES [dbo].[catalogue_richlayer]([id]),
  [cql_property]    NVARCHAR(MAX) NOT NULL,
  [label]           NVARCHAR(MAX) NOT NULL,
  [data_type]       NVARCHAR(MAX) NOT NULL,
  [controller_type] NVARCHAR(MAX) NOT NULL,
  [icon]            NVARCHAR(MAX) NULL,
  [tooltip]         NVARCHAR(MAX) NULL
);

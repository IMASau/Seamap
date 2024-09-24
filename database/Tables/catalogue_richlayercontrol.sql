CREATE TABLE [dbo].[catalogue_richlayercontrol] (
  [id]              INT           NOT NULL PRIMARY KEY IDENTITY(1,1),
  [richlayer_id]    INT           NOT NULL FOREIGN KEY REFERENCES [dbo].[catalogue_richlayer]([id]),
  [cql_property]    NVARCHAR(255) NOT NULL,
  [label]           NVARCHAR(255) NOT NULL,
  [data_type]       NVARCHAR(255) NOT NULL,
  [controller_type] NVARCHAR(255) NOT NULL,
  [icon]            NVARCHAR(255) NULL,
  [tooltip]         NVARCHAR(255) NULL,
  [default_value]   NVARCHAR(255) NULL
);

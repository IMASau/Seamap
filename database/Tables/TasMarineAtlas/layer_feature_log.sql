CREATE TABLE [dbo].[layer_feature_log] (
  [id]        INT           NOT NULL PRIMARY KEY IDENTITY,
  [layer_id]  INT           NOT NULL FOREIGN KEY REFERENCES [dbo].[catalogue_layer]([id]),
  [timestamp] DATETIME      NOT NULL DEFAULT GETDATE(),
  [error]     NVARCHAR(MAX) NULL
);

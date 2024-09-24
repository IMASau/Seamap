CREATE TABLE [dbo].[catalogue_dynamicpill_layers] (
  [id]             INT           NOT NULL IDENTITY(1,1) PRIMARY KEY,
  [layer_id]       INT           NOT NULL FOREIGN KEY REFERENCES [dbo].[catalogue_layer]([id]),
  [dynamicpill_id] INT           NOT NULL FOREIGN KEY REFERENCES [dbo].[catalogue_dynamicpill]([id]),
  [metadata]       NVARCHAR(255) NULL
);

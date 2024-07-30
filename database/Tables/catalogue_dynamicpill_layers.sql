CREATE TABLE [dbo].[catalogue_dynamicpill_layers] (
  [layer_id]       INT NOT NULL FOREIGN KEY REFERENCES [dbo].[catalogue_layer]([id]),
  [dynamicpill_id] INT NOT NULL FOREIGN KEY REFERENCES [dbo].[catalogue_dynamicpill]([id]),
);

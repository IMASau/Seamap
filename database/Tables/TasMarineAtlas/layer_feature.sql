CREATE TABLE [dbo].[layer_feature] (
  [layer_id]   INT           NOT NULL FOREIGN KEY REFERENCES [dbo].[catalogue_layer]([id]),
  [geom]       GEOMETRY      NOT NULL,
);

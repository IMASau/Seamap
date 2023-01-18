CREATE TABLE [dbo].[layer_feature] (
  [layer_id]   INT           NOT NULL FOREIGN KEY REFERENCES [dbo].[catalogue_layer]([id]),
  [feature_id] NVARCHAR(MAX) NOT NULL,
  [geom]       GEOMETRY      NOT NULL,
  [type]       NVARCHAR(MAX) NOT NULL,
);

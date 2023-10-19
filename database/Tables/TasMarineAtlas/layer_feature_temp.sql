-- Is a temporary storage for retrieved layer feature data, prior to being copied
-- into [layer_feature].
CREATE TABLE [dbo].[layer_feature_temp] (
  [id]       INT      NOT NULL PRIMARY KEY IDENTITY,
  [layer_id] INT      NOT NULL FOREIGN KEY REFERENCES [dbo].[catalogue_layer]([id]),
  [geom]     GEOMETRY NOT NULL,
);

CREATE TABLE [dbo].[layer_feature] (
  [id]       INT      NOT NULL PRIMARY KEY IDENTITY,
  [layer_id] INT      NOT NULL FOREIGN KEY REFERENCES [dbo].[catalogue_layer]([id]),
  [geom]     GEOMETRY NOT NULL,
);

CREATE SPATIAL INDEX [layer_geom] ON [dbo].[layer_feature]([geom])
WITH (
  BOUNDING_BOX=( -180, -90, 180, 90 ) 
);

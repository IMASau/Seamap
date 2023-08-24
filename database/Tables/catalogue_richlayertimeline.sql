CREATE TABLE [dbo].[catalogue_richlayertimeline] (
  [id]           INT           NOT NULL PRIMARY KEY IDENTITY(1,1),
  [richlayer_id] INT           NOT NULL FOREIGN KEY REFERENCES [dbo].[catalogue_richlayer]([id]),
  [layer_id]     INT           NOT NULL FOREIGN KEY REFERENCES [dbo].[catalogue_layer]([id]),
  [value]        FLOAT         NOT NULL,
  [label]        NVARCHAR(MAX) NOT NULL
);

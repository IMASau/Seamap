CREATE TABLE [dbo].[catalogue_richlayeralternateview] (
    [id]        INT          NOT NULL PRIMARY KEY IDENTITY(1,1),
    [layer_id]  INT          NOT NULL FOREIGN KEY REFERENCES [dbo].[catalogue_layer]([id]),
    [parent_id] INT          NOT NULL FOREIGN KEY REFERENCES [dbo].[catalogue_layer]([id]),
    [sort_key]  NVARCHAR(10) NULL
);

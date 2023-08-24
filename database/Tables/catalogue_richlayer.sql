CREATE TABLE [dbo].[catalogue_richlayer] (
  [id]           INT           NOT NULL PRIMARY KEY IDENTITY(1,1),
  [layer_id]     INT           NOT NULL FOREIGN KEY REFERENCES [dbo].[catalogue_layer]([id]),
  [tab_label]    NVARCHAR(MAX) NOT NULL,
  [slider_label] NVARCHAR(MAX) NOT NULL,
  [icon]         NVARCHAR(MAX) NOT NULL,
  [tooltip]      NVARCHAR(MAX) NOT NULL
);

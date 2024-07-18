CREATE TABLE [dbo].[catalogue_dynamicpill] (
  [id]                             INT           NOT NULL PRIMARY KEY IDENTITY(1,1),
  [text]                           NVARCHAR(255) NOT NULL,
  [icon]                           NVARCHAR(255) NULL,
  [tooltip]                        NVARCHAR(255) NULL,
  [url]                            NVARCHAR(255) NOT NULL,
  [region_control_cql_property]    NVARCHAR(255) NOT NULL,
  [region_control_label]           NVARCHAR(255) NOT NULL,
  [region_control_data_type]       NVARCHAR(255) NOT NULL,
  [region_control_controller_type] NVARCHAR(255) NOT NULL,
  [region_control_icon]            NVARCHAR(255) NULL,
  [region_control_tooltip]         NVARCHAR(255) NULL,
  [region_control_default_value]   NVARCHAR(255) NULL
);

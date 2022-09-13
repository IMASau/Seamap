create table TRANSFORM_SQUIDLE_DEPLOYMENT_POINTS (
    ID int NOT NULL,
	geom geometry NULL,
	[key] nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	name nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	campaign_name nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	campaign_key nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	campaign_id numeric(9,0) NULL,
	color nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	platform_name nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	platform_key nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	platform_id numeric(9,0) NULL,
	url nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	[date] date NULL,
	media_count int NULL,
	total_annotation_count int NULL,
	public_annotation_count int NULL
);

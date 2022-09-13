create table EXTRACT_SQUIDLE_DEPLOYMENT_POINTS (
	FID nvarchar(255) NULL,
	id nvarchar(255) NOT NULL primary key,
	[key] nvarchar(255) NULL,
	name nvarchar(255) NULL,
	campaign_name nvarchar(255) NULL,
	campaign_key nvarchar(255) NULL,
	campaign_id nvarchar(255) NULL,
	color nvarchar(255) NULL,
	platform_name nvarchar(255) NULL,
	platform_key nvarchar(255) NULL,
	platform_id nvarchar(255) NULL,
	url nvarchar(255) NULL,
	point nvarchar(255) NULL,
	[date] nvarchar(255) NULL,
	media_count nvarchar(255) NULL,
	total_annotation_count nvarchar(255) NULL,
	public_annotation_count nvarchar(255) NULL,
    geom geometry NULL
);

--drop table EXTRACT_SQUIDLE_DEPLOYMENT_POINTS;

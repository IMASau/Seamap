CREATE TABLE TRANSFORM_GLOBALARCHIVE_DEPLOYMENT_POINTS (
	FID nvarchar(255) NOT NULL PRIMARY KEY,
    campaign_id int NULL,
    deployment_id nvarchar(255) null,
    deployment_time datetime NULL,
    description nvarchar(255) NULL,
    [depth] float NULL,
    site nvarchar(255) NULL,
    status nvarchar(255) NULL,
    successful_count nvarchar(255) NULL,
    successful_length nvarchar(255) NULL,
    coordinates nvarchar(255) NULL,
    observer nvarchar(255) NULL,
    location nvarchar(255) NULL,
    campaign_name nvarchar(255) NULL,
    campaign_link nvarchar(255) NULL,
    project_name nvarchar(255) NULL,
    method_name nvarchar(255) NULL,
    colour nvarchar(255) NULL,
	geom geometry NULL
);

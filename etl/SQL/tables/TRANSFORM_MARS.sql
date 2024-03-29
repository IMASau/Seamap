create table TRANSFORM_MARS (
    ogr_fid int IDENTITY(1,1) primary key,
	geom geometry NULL,
	FID nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	SURVEY_ID nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	SURVEY_NAME nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	BASE_DEPTH_M float NULL,
	SAMPLE_LOCATION nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	ENO nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	SAMPLENO nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	SAMPLE_ID nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	IGSN nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	SAMPLE_TYPE nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	SAMPLING_METHOD nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	MATERIAL_CLASS nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	ACQUIRE_DATE DATE NULL,
	LONG_GDA94 float NULL,
	LAT_GDA94 float NULL,
	WATER_DEPTH_M FLOAT NULL,
	ANALYSIS_METHOD nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	MUD_PERCENT FLOAT NULL,
	SAND_PERCENT FLOAT NULL,
	GRAVEL_PERCENT FLOAT NULL,
	CARBONATE_PERCENT FLOAT NULL,
	BIOGENIC_SILICA_PERCENT FLOAT NULL,
	FOLK_CLASS nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	MEAN_GRAIN_SIZE FLOAT NULL,
	SORTING FLOAT NULL,
	SKEWNESS FLOAT NULL,
	KURTOSIS FLOAT NULL,
	DOCUMENTS nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	ARCHIVE_SAMPLE nvarchar(255) COLLATE Latin1_General_CI_AS NULL,
	BATHYMETRY_250 FLOAT NULL,
	BATHYMETRY_50 FLOAT NULL,
	TOP_DEPTH_M FLOAT NULL
);

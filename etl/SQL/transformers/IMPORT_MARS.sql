-- =============================================
-- Author:		Khanh Nguyen
-- Create date: 12/09/2022
-- Description:	Procedure to transform data for MARS
-- =============================================
CREATE OR ALTER PROCEDURE [dbo].[IMPORT_MARS]
AS
BEGIN

	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON;
    BEGIN TRANSACTION
    BEGIN TRY
        DECLARE @DECREASE_THRESHOLD FLOAT = 0.05
        DECLARE @PREVIOUS_TRANSFORMED_COUNT INT
        DECLARE @CURRENT_EXTRACTED_COUNT INT

        SELECT @CURRENT_EXTRACTED_COUNT = COUNT(*) FROM EXTRACT_MARS;
        SELECT @PREVIOUS_TRANSFORMED_COUNT = COUNT(*) FROM TRANSFORM_MARS;
        IF @CURRENT_EXTRACTED_COUNT < @PREVIOUS_TRANSFORMED_COUNT * (1 - @DECREASE_THRESHOLD)
        BEGIN
            SELECT
            -1 AS Status,
            'EXTRACT_MARS records count decreases more than 5% compared to TRANSFORM_MARS' AS ErrorMessage
            RETURN
        END
        TRUNCATE TABLE TRANSFORM_MARS

        -- Vars for cursor
        DECLARE @FID nvarchar(255)
        DECLARE @SURVEY_ID nvarchar(255)
        DECLARE @SURVEY_NAME nvarchar(500)
        DECLARE @BASE_DEPTH_M nvarchar(255)
        DECLARE @SAMPLE_LOCATION nvarchar(255)
        DECLARE @ENO nvarchar(255)
        DECLARE @SAMPLENO nvarchar(255)
        DECLARE @SAMPLE_ID nvarchar(255)
        DECLARE @IGSN nvarchar(255)
        DECLARE @SAMPLE_TYPE nvarchar(255)
        DECLARE @SAMPLING_METHOD nvarchar(255)
        DECLARE @MATERIAL_CLASS nvarchar(255)
        DECLARE @ACQUIRE_DATE nvarchar(255)
        DECLARE @LONG_GDA94 nvarchar(255)
        DECLARE @LAT_GDA94 nvarchar(255)
        DECLARE @WATER_DEPTH_M nvarchar(255)
        DECLARE @ANALYSIS_METHOD nvarchar(255)
        DECLARE @MUD_PERCENT nvarchar(255)
        DECLARE @SAND_PERCENT nvarchar(255)
        DECLARE @GRAVEL_PERCENT nvarchar(255)
        DECLARE @CARBONATE_PERCENT nvarchar(255)
        DECLARE @BIOGENIC_SILICA_PERCENT nvarchar(255)
        DECLARE @FOLK_CLASS nvarchar(255)
        DECLARE @MEAN_GRAIN_SIZE nvarchar(255)
        DECLARE @SORTING nvarchar(255)
        DECLARE @SKEWNESS nvarchar(255)
        DECLARE @KURTOSIS nvarchar(255)
        DECLARE @DOCUMENTS nvarchar(255)
        DECLARE @ARCHIVE_SAMPLE nvarchar(255)
        DECLARE @BATHYMETRY_250 nvarchar(255)
        DECLARE @BATHYMETRY_50 nvarchar(255)
        DECLARE @TOP_DEPTH_M nvarchar(255)
        DECLARE @GEOM geometry


        -- Vars for conversion
        DECLARE @destBASE_DEPTH_M float
        DECLARE @destACQUIRE_DATE DATE
        DECLARE @destLONG_GDA94 float
        DECLARE @destLAT_GDA94 float
        DECLARE @destWATER_DEPTH_M FLOAT
        DECLARE @destMUD_PERCENT FLOAT
        DECLARE @destSAND_PERCENT FLOAT
        DECLARE @destGRAVEL_PERCENT FLOAT
        DECLARE @destCARBONATE_PERCENT FLOAT
        DECLARE @destBIOGENIC_SILICA_PERCENT FLOAT
        DECLARE @destMEAN_GRAIN_SIZE FLOAT
        DECLARE @destSORTING FLOAT
        DECLARE @destSKEWNESS FLOAT
        DECLARE @destKURTOSIS FLOAT
        DECLARE @destBATHYMETRY_250 FLOAT
        DECLARE @destBATHYMETRY_50 FLOAT
        DECLARE @destTOP_DEPTH_M FLOAT

        DECLARE cMARSDATA CURSOR FOR
            SELECT FID, SURVEY_ID, SURVEY_NAME, BASE_DEPTH_M, SAMPLE_LOCATION, ENO, SAMPLENO, SAMPLE_ID, IGSN, SAMPLE_TYPE,
                   SAMPLING_METHOD, MATERIAL_CLASS, ACQUIRE_DATE, LONG_GDA94, LAT_GDA94, WATER_DEPTH_M, ANALYSIS_METHOD,
                   MUD_PERCENT, SAND_PERCENT, GRAVEL_PERCENT, CARBONATE_PERCENT, BIOGENIC_SILICA_PERCENT, FOLK_CLASS,
                   MEAN_GRAIN_SIZE, SORTING, SKEWNESS, KURTOSIS, DOCUMENTS, ARCHIVE_SAMPLE, BATHYMETRY_250, BATHYMETRY_50,
                   TOP_DEPTH_M, GEOM
            FROM EXTRACT_MARS ;

        OPEN cMARSDATA

        FETCH NEXT FROM cMARSDATA INTO @FID, @SURVEY_ID, @SURVEY_NAME, @BASE_DEPTH_M, @SAMPLE_LOCATION, @ENO, @SAMPLENO, @SAMPLE_ID, @IGSN, @SAMPLE_TYPE,
            @SAMPLING_METHOD, @MATERIAL_CLASS, @ACQUIRE_DATE, @LONG_GDA94, @LAT_GDA94, @WATER_DEPTH_M, @ANALYSIS_METHOD,
            @MUD_PERCENT, @SAND_PERCENT, @GRAVEL_PERCENT, @CARBONATE_PERCENT, @BIOGENIC_SILICA_PERCENT, @FOLK_CLASS,
            @MEAN_GRAIN_SIZE, @SORTING, @SKEWNESS, @KURTOSIS, @DOCUMENTS, @ARCHIVE_SAMPLE, @BATHYMETRY_250, @BATHYMETRY_50,
            @TOP_DEPTH_M, @GEOM
        WHILE @@FETCH_STATUS = 0
        BEGIN

            set @destBASE_DEPTH_M = CONVERT(FLOAT, @BASE_DEPTH_M)
            set @destACQUIRE_DATE = CONVERT(DATE, @ACQUIRE_DATE)
            SET @destLONG_GDA94 = CONVERT(FLOAT, @LONG_GDA94)
            SET @destLAT_GDA94 = CONVERT(FLOAT, @LAT_GDA94)
            SET @destWATER_DEPTH_M = CONVERT(FLOAT, @WATER_DEPTH_M)
            SET @destMUD_PERCENT = CONVERT(FLOAT, @MUD_PERCENT)
            SET @destSAND_PERCENT = CONVERT(FLOAT, @SAND_PERCENT)
            SET @destGRAVEL_PERCENT = CONVERT(FLOAT, @GRAVEL_PERCENT)
            SET @destCARBONATE_PERCENT = CONVERT(FLOAT, @CARBONATE_PERCENT)
            SET @destBIOGENIC_SILICA_PERCENT = CONVERT(FLOAT, @BIOGENIC_SILICA_PERCENT)
            SET @destMEAN_GRAIN_SIZE = CONVERT(FLOAT, @MEAN_GRAIN_SIZE)
            SET @destSORTING = CONVERT(FLOAT, @SORTING)
            SET @destSKEWNESS = CONVERT(FLOAT, @SKEWNESS)
            SET @destKURTOSIS = CONVERT(FLOAT, @KURTOSIS)
            SET @destBATHYMETRY_250 = CONVERT(FLOAT, @BATHYMETRY_250)
            SET @destBATHYMETRY_50 = CONVERT(FLOAT, @BATHYMETRY_50)
            SET @destTOP_DEPTH_M = CONVERT(FLOAT, @TOP_DEPTH_M)


        INSERT INTO TRANSFORM_MARS
               (geom, FID, SURVEY_ID, SURVEY_NAME, BASE_DEPTH_M, SAMPLE_LOCATION, ENO, SAMPLENO, SAMPLE_ID, IGSN, SAMPLE_TYPE, SAMPLING_METHOD, MATERIAL_CLASS, ACQUIRE_DATE, LONG_GDA94, LAT_GDA94,
                WATER_DEPTH_M, ANALYSIS_METHOD, MUD_PERCENT, SAND_PERCENT, GRAVEL_PERCENT, CARBONATE_PERCENT, BIOGENIC_SILICA_PERCENT, FOLK_CLASS, MEAN_GRAIN_SIZE, SORTING, SKEWNESS, KURTOSIS, DOCUMENTS, ARCHIVE_SAMPLE,
                BATHYMETRY_250, BATHYMETRY_50, TOP_DEPTH_M)
         VALUES
               (@GEOM, @FID, @SURVEY_ID, @SURVEY_NAME, @destBASE_DEPTH_M, @SAMPLE_LOCATION, @ENO, @SAMPLENO, @SAMPLE_ID, @IGSN, @SAMPLE_TYPE, @SAMPLING_METHOD, @MATERIAL_CLASS, @destACQUIRE_DATE, @destLONG_GDA94, @destLAT_GDA94,
                @destWATER_DEPTH_M, @ANALYSIS_METHOD, @destMUD_PERCENT, @destSAND_PERCENT, @destGRAVEL_PERCENT, @destCARBONATE_PERCENT, @destBIOGENIC_SILICA_PERCENT, @FOLK_CLASS, @destMEAN_GRAIN_SIZE, @destSORTING, @destSKEWNESS, @destKURTOSIS,
                @DOCUMENTS, @ARCHIVE_SAMPLE, @destBATHYMETRY_250, @destBATHYMETRY_50, @destTOP_DEPTH_M)

        FETCH NEXT FROM cMARSDATA INTO @FID, @SURVEY_ID, @SURVEY_NAME, @BASE_DEPTH_M, @SAMPLE_LOCATION, @ENO, @SAMPLENO, @SAMPLE_ID, @IGSN, @SAMPLE_TYPE,
            @SAMPLING_METHOD, @MATERIAL_CLASS, @ACQUIRE_DATE, @LONG_GDA94, @LAT_GDA94, @WATER_DEPTH_M, @ANALYSIS_METHOD,
            @MUD_PERCENT, @SAND_PERCENT, @GRAVEL_PERCENT, @CARBONATE_PERCENT, @BIOGENIC_SILICA_PERCENT, @FOLK_CLASS,
            @MEAN_GRAIN_SIZE, @SORTING, @SKEWNESS, @KURTOSIS, @DOCUMENTS, @ARCHIVE_SAMPLE, @BATHYMETRY_250, @BATHYMETRY_50,
            @TOP_DEPTH_M, @GEOM
        END

        CLOSE cMARSDATA
        DEALLOCATE cMARSDATA
    END TRY
    BEGIN CATCH
        ROLLBACK TRANSACTION
        SELECT
            -1 AS Status,
            ERROR_NUMBER() AS ErrorNumber,
            ERROR_SEVERITY() AS ErrorSeverity,
            ERROR_STATE() AS ErrorState,
            ERROR_PROCEDURE() AS ErrorProcedure,
            ERROR_MESSAGE() AS ErrorMessage,
            ERROR_LINE() AS ErrorLine;
        RETURN
    end catch
    COMMIT TRANSACTION
    SELECT 0 As Status
END

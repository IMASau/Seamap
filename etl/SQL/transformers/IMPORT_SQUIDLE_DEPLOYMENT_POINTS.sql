-- =============================================
-- Author:		Khanh Nguyen
-- Create date: 12/09/2022
-- Description:	Procedure to transform data for SQUIDLE deployment points
-- =============================================
CREATE OR ALTER PROCEDURE [dbo].[IMPORT_SQUIDLE_DEPLOYMENT_POINTS]
AS
BEGIN
    -- SET NOCOUNT ON added to prevent extra result sets from
    -- interfering with SELECT statements.
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRANSACTION
    BEGIN TRY
        DECLARE @DECREASE_THRESHOLD FLOAT = 0.05
        DECLARE @PREVIOUS_TRANSFORMED_COUNT INT
        DECLARE @CURRENT_EXTRACTED_COUNT INT

        SELECT @CURRENT_EXTRACTED_COUNT = COUNT(*) FROM EXTRACT_SQUIDLE_DEPLOYMENT_POINTS;
        SELECT @PREVIOUS_TRANSFORMED_COUNT = COUNT(*) FROM TRANSFORM_SQUIDLE_DEPLOYMENT_POINTS;
        IF @CURRENT_EXTRACTED_COUNT < @PREVIOUS_TRANSFORMED_COUNT * (1 - @DECREASE_THRESHOLD)
        BEGIN
          RAISERROR('EXTRACT_SQUIDLE_DEPLOYMENT_POINTS records count decreases more than 5%% compared to TRANSFORM_SQUIDLE_DEPLOYMENT_POINTS', 16, 1)
        END
        TRUNCATE TABLE TRANSFORM_SQUIDLE_DEPLOYMENT_POINTS




        -- Vars for cursor
        DECLARE @FID nvarchar(255)
        DECLARE @ID nvarchar(255)
        DECLARE @KEY nvarchar(255)
        DECLARE @NAME nvarchar(255)
        DECLARE @CAMPAIGN_KEY nvarchar(255)
        DECLARE @CAMPAIGN_NAME nvarchar(255)
        DECLARE @CAMPAIGN_ID nvarchar(255)
        DECLARE @COLOR nvarchar(255)
        DECLARE @DEP_MIN nvarchar(255)
        DECLARE @DEP_MAX nvarchar(255)  
        DECLARE @ALT_MIN nvarchar(255)
        DECLARE @ALT_MAX nvarchar(255)              
        DECLARE @TIMESTAMP_START nvarchar(255)   
        DECLARE @TIMESTAMP_END nvarchar(255)                
        DECLARE @PLATFORM_NAME nvarchar(255)
        DECLARE @PLATFORM_KEY nvarchar(255)
        DECLARE @PLATFORM_ID nvarchar(255)
        DECLARE @POINT nvarchar(255)
        DECLARE @DATE nvarchar(255)
        DECLARE @MEDIA_COUNT nvarchar(255)
        DECLARE @POSE_COUNT nvarchar(255)            
        DECLARE @TOTAL_ANNOTATION_COUNT nvarchar(255)
        DECLARE @PUBLIC_ANNOTATION_COUNT nvarchar(255)
        DECLARE @URL nvarchar(255)            
        DECLARE @GEOM geometry


        -- Vars for conversion
        DECLARE @destID int
        DECLARE @destCAMPAIGN_ID numeric(9, 0)
        DECLARE @destPLATFORM_ID numeric(9, 0)
        DECLARE @destDEP_MIN float
        DECLARE @destDEP_MAX float    
        DECLARE @destALT_MIN float
        DECLARE @destALT_MAX float  
        DECLARE @destTIMESTAMP_START datetime2(0)   
        DECLARE @destTIMESTAMP_END datetime2(0)            
        DECLARE @destDATE date
        DECLARE @destMEDIA_COUNT int
        DECLARE @destPOSE_COUNT int            
        DECLARE @destTOTAL_ANNOTATION_COUNT int
        DECLARE @destPUBLIC_ANNOTATION_COUNT int


        DECLARE cSQUIDLEDATA CURSOR LOCAL FOR
            SELECT FID, ID, [KEY], NAME, CAMPAIGN_NAME, CAMPAIGN_KEY, CAMPAIGN_ID, COLOR, DEP_MIN, DEP_MAX, ALT_MIN, ALT_MAX, TIMESTAMP_START, TIMESTAMP_END, PLATFORM_NAME, PLATFORM_KEY, PLATFORM_ID,
                   POINT, [DATE], MEDIA_COUNT, POSE_COUNT, TOTAL_ANNOTATION_COUNT, PUBLIC_ANNOTATION_COUNT, URL, GEOM
            FROM EXTRACT_SQUIDLE_DEPLOYMENT_POINTS ;

        OPEN cSQUIDLEDATA

        FETCH NEXT FROM cSQUIDLEDATA INTO @FID, @ID, @KEY, @NAME, @CAMPAIGN_NAME, @CAMPAIGN_KEY, @CAMPAIGN_ID, @COLOR, @DEP_MIN, @DEP_MAX, @ALT_MIN, @ALT_MAX, @TIMESTAMP_START, @TIMESTAMP_END, @PLATFORM_NAME, @PLATFORM_KEY, @PLATFORM_ID,
                        @POINT, @DATE, @MEDIA_COUNT, @POSE_COUNT, @TOTAL_ANNOTATION_COUNT, @PUBLIC_ANNOTATION_COUNT, @URL, @GEOM
        WHILE @@FETCH_STATUS = 0
        BEGIN


            SET @destID = TRY_PARSE(@ID as INT)
            SET @destCAMPAIGN_ID = TRY_PARSE(@CAMPAIGN_ID as NUMERIC(9,0))
            SET @destPLATFORM_ID = TRY_PARSE(@PLATFORM_ID as NUMERIC(9,0))
            SET @destDEP_MIN = TRY_PARSE(@DEP_MIN as FLOAT)
            SET @destDEP_MAX = TRY_PARSE(@DEP_MAX as FLOAT)   
            SET @destALT_MIN = TRY_PARSE(@ALT_MIN as FLOAT)
            SET @destALT_MAX = TRY_PARSE(@ALT_MAX as FLOAT)  
            SET @destTIMESTAMP_START = TRY_CONVERT(datetime2(0), @TIMESTAMP_START);
            SET @destTIMESTAMP_END = TRY_CONVERT(datetime2(0), @TIMESTAMP_END);
            SET @destDATE = TRY_PARSE(@DATE as DATE)
            SET @destMEDIA_COUNT = TRY_PARSE(@MEDIA_COUNT as INT)
            SET @destPOSE_COUNT = TRY_PARSE(@POSE_COUNT as INT)            
            SET @destTOTAL_ANNOTATION_COUNT = TRY_PARSE(@TOTAL_ANNOTATION_COUNT as INT)
            SET @destPUBLIC_ANNOTATION_COUNT = TRY_PARSE(@PUBLIC_ANNOTATION_COUNT as INT)
            IF @GEOM IS NOT NULL
                SET @GEOM.STSrid = 3112

        INSERT INTO TRANSFORM_SQUIDLE_DEPLOYMENT_POINTS
               (ID,geom,[key],name,campaign_name,campaign_key,campaign_id,color,dep_min,dep_max,alt_min,alt_max,timestamp_start,timestamp_end,platform_name,platform_key,platform_id,[date],media_count,pose_count,total_annotation_count,public_annotation_count,url)
         VALUES
               (@destID, @GEOM, @KEY, @NAME, @CAMPAIGN_NAME, @CAMPAIGN_KEY, @destCAMPAIGN_ID, @COLOR, @destDEP_MIN, @destDEP_MAX, @destALT_MIN, @destALT_MAX, @destTIMESTAMP_START, @destTIMESTAMP_END, @PLATFORM_NAME, @PLATFORM_KEY, @destPLATFORM_ID, @destDATE, @destMEDIA_COUNT, @destPOSE_COUNT, @destTOTAL_ANNOTATION_COUNT, @destPUBLIC_ANNOTATION_COUNT, @URL)

            FETCH NEXT FROM cSQUIDLEDATA INTO @FID, @ID, @KEY, @NAME, @CAMPAIGN_NAME, @CAMPAIGN_KEY, @CAMPAIGN_ID, @COLOR, @DEP_MIN, @DEP_MAX, @ALT_MIN, @ALT_MAX, @TIMESTAMP_START, @TIMESTAMP_END, @PLATFORM_NAME, @PLATFORM_KEY, @PLATFORM_ID,
                        @POINT, @DATE, @MEDIA_COUNT, @POSE_COUNT, @TOTAL_ANNOTATION_COUNT, @PUBLIC_ANNOTATION_COUNT, @URL, @GEOM
        END

        CLOSE cSQUIDLEDATA
        DEALLOCATE cSQUIDLEDATA
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION
         SELECT
            -1 AS Status,
            ERROR_NUMBER() AS ErrorNumber,
            ERROR_SEVERITY() AS ErrorSeverity,
            ERROR_STATE() AS ErrorState,
            ERROR_PROCEDURE() AS ErrorProcedure,
            ERROR_MESSAGE() AS ErrorMessage,
            ERROR_LINE() AS ErrorLine;
        RETURN
    END catch
    COMMIT TRANSACTION

    SELECT 0 as Status
END


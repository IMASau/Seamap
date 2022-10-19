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
    BEGIN TRANSACTION
    BEGIN TRY
        DECLARE @DECREASE_THRESHOLD FLOAT = 0.05
        DECLARE @PREVIOUS_TRANSFORMED_COUNT INT
        DECLARE @CURRENT_EXTRACTED_COUNT INT

        SELECT @CURRENT_EXTRACTED_COUNT = COUNT(*) FROM EXTRACT_SQUIDLE_DEPLOYMENT_POINTS;
        SELECT @PREVIOUS_TRANSFORMED_COUNT = COUNT(*) FROM TRANSFORM_SQUIDLE_DEPLOYMENT_POINTS;
        IF @CURRENT_EXTRACTED_COUNT < @PREVIOUS_TRANSFORMED_COUNT * (1 - @DECREASE_THRESHOLD)
        BEGIN
            SELECT
            -1 AS Status,
            'EXTRACT_SQUIDLE_DEPLOYMENT_POINTS records count decreases more than 5% compared to TRANSFORM_SQUIDLE_DEPLOYMENT_POINTS' AS ErrorMessage
            RETURN
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
        DECLARE @PLATFORM_NAME nvarchar(255)
        DECLARE @PLATFORM_KEY nvarchar(255)
        DECLARE @PLATFORM_ID nvarchar(255)
        DECLARE @URL nvarchar(255)
        DECLARE @POINT nvarchar(255)
        DECLARE @DATE nvarchar(255)
        DECLARE @MEDIA_COUNT nvarchar(255)
        DECLARE @TOTAL_ANNOTATION_COUNT nvarchar(255)
        DECLARE @PUBLIC_ANNOTATION_COUNT nvarchar(255)
        DECLARE @GEOM geometry


        -- Vars for conversion
        DECLARE @destID int
        DECLARE @destCAMPAIGN_ID numeric(9, 0)
        DECLARE @destPLATFORM_ID numeric(9, 0)
        DECLARE @destDATE date
        DECLARE @destMEDIA_COUNT int
        DECLARE @destTOTAL_ANNOTATION_COUNT int
        DECLARE @destPUBLIC_ANNOTATION_COUNT int


        DECLARE cSQUIDLEDATA CURSOR LOCAL FOR
            SELECT FID, ID, [KEY], NAME, CAMPAIGN_KEY, CAMPAIGN_ID, COLOR, PLATFORM_NAME, PLATFORM_KEY, PLATFORM_ID, URL,
                   POINT, [DATE], MEDIA_COUNT, TOTAL_ANNOTATION_COUNT, PUBLIC_ANNOTATION_COUNT, GEOM
            FROM EXTRACT_SQUIDLE_DEPLOYMENT_POINTS ;

        OPEN cSQUIDLEDATA

        FETCH NEXT FROM cSQUIDLEDATA INTO @FID, @ID, @KEY, @NAME, @CAMPAIGN_KEY, @CAMPAIGN_ID, @COLOR, @PLATFORM_NAME, @PLATFORM_KEY, @PLATFORM_ID, @URL,
                        @POINT, @DATE, @MEDIA_COUNT, @TOTAL_ANNOTATION_COUNT, @PUBLIC_ANNOTATION_COUNT, @GEOM
        WHILE @@FETCH_STATUS = 0
        BEGIN


            SET @destID = CONVERT(INT, @ID)
            SET @destCAMPAIGN_ID = CONVERT(NUMERIC(9,0), @CAMPAIGN_ID)
            SET @destPLATFORM_ID = CONVERT(NUMERIC(9,0), @PLATFORM_ID)
            SET @destDATE = CONVERT(DATE, @DATE)
            SET @destMEDIA_COUNT = CONVERT(INT, @MEDIA_COUNT)
            SET @destTOTAL_ANNOTATION_COUNT = CONVERT(INT, @TOTAL_ANNOTATION_COUNT)
            SET @destPUBLIC_ANNOTATION_COUNT = CONVERT(INT, @PUBLIC_ANNOTATION_COUNT)

        INSERT INTO TRANSFORM_SQUIDLE_DEPLOYMENT_POINTS
               (ID,geom,[key],name,campaign_name,campaign_key,campaign_id,color,platform_name,platform_key,platform_id,url,[date],media_count,total_annotation_count,public_annotation_count)
         VALUES
               (@destID, @GEOM, @KEY, @NAME, @CAMPAIGN_NAME, @CAMPAIGN_KEY, @destCAMPAIGN_ID, @COLOR, @PLATFORM_NAME, @PLATFORM_KEY, @destPLATFORM_ID, @URL, @DATE, @destMEDIA_COUNT, @destTOTAL_ANNOTATION_COUNT, @destPUBLIC_ANNOTATION_COUNT)

            FETCH NEXT FROM cSQUIDLEDATA INTO @FID, @ID, @KEY, @NAME, @CAMPAIGN_KEY, @CAMPAIGN_ID, @COLOR, @PLATFORM_NAME, @PLATFORM_KEY, @PLATFORM_ID, @URL,
                        @POINT, @DATE, @MEDIA_COUNT, @TOTAL_ANNOTATION_COUNT, @PUBLIC_ANNOTATION_COUNT, @GEOM
        END

        CLOSE cSQUIDLEDATA
        DEALLOCATE cSQUIDLEDATA
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

    SELECT 0 as Status
END


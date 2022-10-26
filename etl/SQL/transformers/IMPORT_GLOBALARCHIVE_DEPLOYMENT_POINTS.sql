-- =============================================
-- Author:		Khanh Nguyen
-- Create date: 12/09/2022
-- Description:	Procedure to transform data for Global Archive Deployment Points
-- =============================================
CREATE OR ALTER PROCEDURE [dbo].[IMPORT_GLOBALARCHIVE_DEPLOYMENT_POINTS]
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

        SELECT @CURRENT_EXTRACTED_COUNT = COUNT(*) FROM EXTRACT_GLOBALARCHIVE_DEPLOYMENT_POINTS;
        SELECT @PREVIOUS_TRANSFORMED_COUNT = COUNT(*) FROM TRANSFORM_GLOBALARCHIVE_DEPLOYMENT_POINTS;
        IF @CURRENT_EXTRACTED_COUNT < @PREVIOUS_TRANSFORMED_COUNT * (1 - @DECREASE_THRESHOLD)
        BEGIN
            SELECT
            -1 AS Status,
            'EXTRACT_GLOBALARCHIVE_DEPLOYMENT_POINTS records count decreases more than 5% compared to TRANSFORM_GLOBALARCHIVE_DEPLOYMENT_POINTS' AS ErrorMessage
            RETURN
        END
        TRUNCATE TABLE EXTRACT_GLOBALARCHIVE_DEPLOYMENT_POINTS

        -- Vars for cursor
        DECLARE @FID nvarchar(255)
        DECLARE @CAMPAIGN_ID nvarchar(255)
        DECLARE @DEPLOYMENT_ID nvarchar(255)
        DECLARE @DEPLOYMENT_TIME nvarchar(255)
        DECLARE @DESCRIPTION nvarchar(255)
        DECLARE @DEPTH nvarchar(255)
        DECLARE @SITE nvarchar(255)
        DECLARE @STATUS nvarchar(255)
        DECLARE @SUCCESSFUL_COUNT nvarchar(255)
        DECLARE @SUCCESSFUL_LENGTH nvarchar(255)
        DECLARE @COORDINATES nvarchar(255)
        DECLARE @OBSERVER nvarchar(255)
        DECLARE @LOCATION nvarchar(255)
        DECLARE @CAMPAIGN_NAME nvarchar(255)
        DECLARE @CAMPAIGN_LINK nvarchar(255)
        DECLARE @PROJECT_NAME nvarchar(255)
        DECLARE @METHOD_NAME nvarchar(255)
        DECLARE @COLOUR nvarchar(255)
        DECLARE @GEOM geometry


        -- Vars for lookup ID's
        DECLARE @destCAMPAIGN_ID int
        DECLARE @destDEPLOYMENT_TIME datetime
        DECLARE @destDEPTH float

        DECLARE cGLOBALARCHIVEDATA CURSOR FOR
            SELECT FID, CAMPAIGN_ID, DEPLOYMENT_ID, DEPLOYMENT_TIME, DESCRIPTION, [DEPTH], SITE, STATUS,
                   SUCCESSFUL_COUNT, SUCCESSFUL_LENGTH, COORDINATES, OBSERVER, LOCATION, CAMPAIGN_NAME, CAMPAIGN_LINK, PROJECT_NAME, METHOD_NAME, COLOUR, GEOM
            FROM EXTRACT_GLOBALARCHIVE_DEPLOYMENT_POINTS;

        OPEN cGLOBALARCHIVEDATA

        FETCH NEXT FROM cGLOBALARCHIVEDATA INTO @FID, @CAMPAIGN_ID, @DEPLOYMENT_ID, @DEPLOYMENT_TIME, @DESCRIPTION, @DEPTH, @SITE, @STATUS,
                   @SUCCESSFUL_COUNT, @SUCCESSFUL_LENGTH, @COORDINATES, @OBSERVER, @LOCATION, @CAMPAIGN_NAME, @CAMPAIGN_LINK, @PROJECT_NAME, @METHOD_NAME, @COLOUR, @GEOM
        WHILE @@FETCH_STATUS = 0
        BEGIN


            SET @destCAMPAIGN_ID = CONVERT(INT, @CAMPAIGN_ID)
            SET @destDEPLOYMENT_TIME = CONVERT(DATETIME, @DEPLOYMENT_TIME)
            SET @destDEPTH = CONVERT(FLOAT, @DEPTH)
            IF @GEOM IS NOT NULL
                SET @GEOM.STSrid = 3112

        INSERT INTO TRANSFORM_GLOBALARCHIVE_DEPLOYMENT_POINTS
            (FID, CAMPAIGN_ID, DEPLOYMENT_ID, DEPLOYMENT_TIME, DESCRIPTION, [DEPTH], SITE, STATUS, SUCCESSFUL_COUNT,
            SUCCESSFUL_LENGTH, COORDINATES, OBSERVER, LOCATION, CAMPAIGN_NAME, CAMPAIGN_LINK, PROJECT_NAME,
            METHOD_NAME, COLOUR, GEOM)
            VALUES
            (@FID, @destCAMPAIGN_ID, @DEPLOYMENT_ID, @destDEPLOYMENT_TIME, @DESCRIPTION, @destDEPTH, @SITE, @STATUS, @SUCCESSFUL_COUNT,
            @SUCCESSFUL_LENGTH, @COORDINATES, @OBSERVER, @LOCATION, @CAMPAIGN_NAME, @CAMPAIGN_LINK, @PROJECT_NAME,
            @METHOD_NAME, @COLOUR, @GEOM)

            FETCH NEXT FROM cGLOBALARCHIVEDATA INTO @FID, @CAMPAIGN_ID, @DEPLOYMENT_ID, @DEPLOYMENT_TIME, @DESCRIPTION, @DEPTH, @SITE, @STATUS,
                @SUCCESSFUL_COUNT, @SUCCESSFUL_LENGTH, @COORDINATES, @OBSERVER, @LOCATION, @CAMPAIGN_NAME, @CAMPAIGN_LINK, @PROJECT_NAME,
                @METHOD_NAME, @COLOUR, @GEOM
        END

        CLOSE cGLOBALARCHIVEDATA
        DEALLOCATE cGLOBALARCHIVEDATA
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
    END CATCH

    COMMIT TRANSACTION
    SELECT 0 as Status

END

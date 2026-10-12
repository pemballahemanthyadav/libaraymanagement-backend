@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF)
@REM Maven Start Up Batch script
@REM ----------------------------------------------------------------------------
@IF "%__MVNW_ARG0_NAME__%"=="" (SET "MAVEN_CMD_LINE_ARGS=%*")

@SET MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%
@IF NOT "%MAVEN_PROJECTBASEDIR%"=="" GOTO endDetectBaseDir

@SET EXEC_DIR=%CD%
@SET WDIR=%EXEC_DIR%
:findBaseDir
IF EXIST "%WDIR%"\.mvn GOTO baseDirFound
cd ..
IF "%WDIR%"=="%CD%" GOTO baseDirNotFound
SET WDIR=%CD%
GOTO findBaseDir

:baseDirFound
SET MAVEN_PROJECTBASEDIR=%WDIR%
cd "%EXEC_DIR%"
GOTO endDetectBaseDir

:baseDirNotFound
SET MAVEN_PROJECTBASEDIR=%EXEC_DIR%
cd "%EXEC_DIR%"

:endDetectBaseDir

SET DISTRIBUTIONURL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip
SET MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6-bin\apache-maven-3.9.6

IF EXIST "%MAVEN_HOME%\bin\mvn.cmd" GOTO startMaven

@ECHO Downloading Apache Maven 3.9.6...
powershell -Command "& { (New-Object Net.WebClient).DownloadFile('%DISTRIBUTIONURL%', '%TEMP%\apache-maven-3.9.6-bin.zip') }"
powershell -Command "& { Expand-Archive '%TEMP%\apache-maven-3.9.6-bin.zip' '%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6-bin' -Force }"

:startMaven
@SET MAVEN_OPTS=%MAVEN_OPTS%
"%MAVEN_HOME%\bin\mvn.cmd" %*

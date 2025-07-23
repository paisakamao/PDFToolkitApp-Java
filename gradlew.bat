
@echo off
set DIR=%~dp0
set APP_HOME=%DIR%

set DEFAULT_JVM_OPTS=

if defined JAVA_HOME (
    set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
    set JAVA_EXE=java
)

"%JAVA_EXE%" %JAVA_OPTS% %DEFAULT_JVM_OPTS% -classpath "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*

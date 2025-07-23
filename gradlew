
#!/usr/bin/env sh

##############################################################################
##
##  Gradle start up script for POSIX compliant shells
##
##############################################################################

# Set the location of the gradle-wrapper.properties file
APP_HOME=$(cd "$(dirname "$0")"; pwd)

DEFAULT_JVM_OPTS=""

# Attempt to find Java executable
if [ -n "$JAVA_HOME" ] ; then
    JAVA_EXE="$JAVA_HOME/bin/java"
else
    JAVA_EXE=java
fi

# Run the actual wrapper jar
exec "$JAVA_EXE" $JAVA_OPTS $DEFAULT_JVM_OPTS -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"

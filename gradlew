#!/bin/sh

##############################################################################
#
#   Gradle start up script for UN*X
#
##############################################################################

# Attempt to set APP_HOME

# Resolve links: $0 may be a link
app_path=$0

# Need this for daisy-chained symlinks.
while
    APP_HOME=${app_path%"${app_path##*/}"}  # leaves a trailing /; empty if no leading path
    [ -h "$app_path" ]
do
    ls=$( ls -ld "$app_path" )
    link=${ls#*' -> '}
    case $link in             #(
      /*)   app_path=$link ;; #(
      *)    app_path=$APP_HOME$link ;;
    esac
done

# This is normally unused
# shellcheck disable=SC2034
APP_BASE_NAME=${0##*/}
# Discard cd here to prevent the user from being able to access the
# executable by running it from a different working directory.
APP_HOME=${APP_HOME:-$PWD}

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Collect all arguments for the java command:
#   * DEFAULT_JVM_OPTS, JAVA_OPTS, and optsEnvironmentVar are automatically handled by the script
#   * --module-path (added by build scripts) is passed preserved
#   * extra arguments where passed through to the main class
set -- \
        "-Dorg.gradle.appname=$APP_BASE_NAME" \
        -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
        org.gradle.wrapper.GradleWrapperMain \
        "$@"

# Stop when "xargs" is not available.
if ! command -v xargs >/dev/null 2>&1
then
    die "xargs is not available"
fi

# Add maximum memory for Post-processor.
# Must be a positive integer, in MB.
GRADLE_MAX_MEMORY=64

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD=$JAVA_HOME/jre/sh/java
    else
        JAVACMD=$JAVA_HOME/bin/java
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD=java
    if ! command -v java >/dev/null 2>&1
    then
        die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
fi

# Increase the maximum file descriptors if we can.
if ! "$cygwin" && ! "$msys" && ! "$nonstop" ; then
    case "$( ulimit -H -n )" in
    *max*) ;;
    *)
        # In POSIX shells, ulimit -H is undefined. That's why the result is "max".
        ulimit -n "$( ulimit -H -n )" || :
    esac
fi

# Escape application args
save() {
    for i do printf '%s\n' "$i" | sed "s/'/'\\\\''/g; s/^/\'/; s/$/\'/"; done
}
OLD_IFS=$IFS
IFS=
DUMPARGS=$(save "$@")
IFS=$OLD_IFS

# Evaluate the arguments in the given list of environment variables
eval "set -- $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \"$@\""

exec "$JAVACMD" "$@"

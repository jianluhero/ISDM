DIR=`pwd`
BASEDIR="`cd .. && pwd`"
PIDS=

# Wait for a regular expression to appear in a file.
# $1 is the log to check
# $2 is the regex to wait for
# $3 is the optional output frequency. Messages will be output every n sleeps. Default 1.
# $4 is the optional sleep time. Defaults to 1 second.
function waitFor {
    SLEEP_TIME=1
    FREQUENCY=1
    if [ ! -z "$3" ]; then
        FREQUENCY=$3
    fi
    if [ ! -z "$4" ]; then
        SLEEP_TIME=$4
    fi
    F=$FREQUENCY
    echo "Waiting for '$1' to exist..."
    while [[ ! -e $1 ]]; do
        if (( --F == 0 )); then
            echo "Still waiting for '$1' to exist..."
            F=$FREQUENCY
        fi
        sleep $SLEEP_TIME
    done
    echo "Waiting for '$2'..."
    while [ -z "`grep \"$2\" \"$1\"`" ]; do
        if (( --F == 0 )); then
            echo "Still waiting for '$2'..."
            F=$FREQUENCY
        fi
        sleep $SLEEP_TIME
    done
}

# Make a classpath argument by looking in a directory of jar files.
# Positional parameters are the directories to look in
function makeClasspath {
    RESULT="../supplement"
    while [[ ! -z "$1" ]]; do
        for NEXT in $1/*.jar; do
            RESULT="$RESULT:$NEXT"
        done
        shift
    done
    CP=${RESULT#:}
}

# Print the usage statement
function printUsage {
    echo "Usage: $0 [options]"
    echo "Options"
    echo "======="
    echo "-m    --map       <mapdir>      Set the map directory. Default is \"$BASEDIR/maps/gml/test\""
    echo "-l    --log       <logdir>      Set the log directory. Default is \"logs\""
    echo "-c    --config    <configdir>   Set the config directory. Default is \"config\""
    echo "-s    --timestamp               Append a timestamp, the team name and map name to the log directory name"
    echo "-t    --team      <teamname>    Set the team name. Default is \"\""
}

# Process arguments
function processArgs {
    LOGDIR="logs"
    MAP="$BASEDIR/maps/gml/test"
    TEAM=""
    TIMESTAMP_LOGS=""
    CONFIGDIR="$DIR/config"

    while [[ ! -z "$1" ]]; do
        case "$1" in
            -m | --map)
                MAP="$2"
                shift 2
                ;;
            -l | --log)
                LOGDIR="$2"
                shift 2
                ;;
            -t | --team)
                TEAM="$2"
                shift 2
                ;;
            -s | --timestamp)
                TIMESTAMP_LOGS="yes";
                shift
                ;;
	    -c | --config)
		CONFIGDIR="$2"
		shift 2
		;;
            -h | --help)
                printUsage
                exit 1;
                ;;
            
            *)
                echo "Unrecognised option: $1"
                printUsage
                exit 1
                ;;
        esac
    done

    if [ -z $MAP ] ; then
        printUsage
        exit 1
    fi
    if [ ! -d $MAP ] ; then
        echo "$MAP is not a directory"
        printUsage
        exit 1
    fi

    if [ ! -z "$TIMESTAMP_LOGS" ] ; then
        TIME="`date +%m%d-%H%M%S`"
        MAPNAME="`basename $MAP`"
        if [ -z "$TEAM" ]; then
            LOGDIR="$LOGDIR/$TIME-$MAPNAME"
        else
            LOGDIR="$LOGDIR/$TIME-$TEAM-$MAPNAME"
        fi
    fi
    LOGDIR=`readlink -f $LOGDIR`
    mkdir -p $LOGDIR
}

# Start the kernel
function startKernel {
    KERNEL_OPTIONS="-c $CONFIGDIR/kernel.cfg --gis.map.dir=$MAP --kernel.logname=$LOGDIR/rescue.log $*"
    makeClasspath $BASEDIR/jars $BASEDIR/lib
    xterm -T kernel -e "java -cp $CP kernel.StartKernel $KERNEL_OPTIONS 2>&1 | tee $LOGDIR/kernel-out.log" &
    PIDS="$PIDS $!"
    # Wait for the kernel to start
    waitFor $LOGDIR/kernel-out.log "Listening for connections"
}

# Start the viewer and simulators
function startSims {
    makeClasspath $BASEDIR/lib
    # Simulators
    xterm -T misc -e "java -Xmx256m -cp $CP:$BASEDIR/jars/rescuecore2.jar:$BASEDIR/jars/standard.jar:$BASEDIR/jars/misc.jar rescuecore2.LaunchComponents misc.MiscSimulator -c $CONFIGDIR/misc.cfg $* 2>&1 | tee $LOGDIR/misc-out.log" &
    PIDS="$PIDS $!"
    xterm -T traffic -e "java -Xmx256m -cp $CP:$BASEDIR/jars/rescuecore2.jar:$BASEDIR/jars/standard.jar:$BASEDIR/jars/traffic3.jar rescuecore2.LaunchComponents traffic3.simulator.TrafficSimulator -c $CONFIGDIR/traffic3.cfg $* 2>&1 | tee $LOGDIR/traffic-out.log" &
    PIDS="$PIDS $!"
    xterm -T fire -e "java -Xmx256m -cp $CP:$BASEDIR/jars/rescuecore2.jar:$BASEDIR/jars/standard.jar:$BASEDIR/jars/resq-fire.jar:$BASEDIR/oldsims/firesimulator/lib/commons-logging-1.1.1.jar rescuecore2.LaunchComponents firesimulator.FireSimulatorWrapper -c $CONFIGDIR/resq-fire.cfg $* 2>&1 | tee $LOGDIR/fire-out.log" &
    PIDS="$PIDS $!"
    xterm -T ignition -e "java -Xmx256m -cp $CP:$BASEDIR/jars/rescuecore2.jar:$BASEDIR/jars/standard.jar:$BASEDIR/jars/ignition.jar rescuecore2.LaunchComponents ignition.IgnitionSimulator -c $CONFIGDIR/ignition.cfg $* 2>&1 | tee $LOGDIR/ignition-out.log" &
    PIDS="$PIDS $!"
    xterm -T collapse -e "java -Xmx256m -cp $CP:$BASEDIR/jars/rescuecore2.jar:$BASEDIR/jars/standard.jar:$BASEDIR/jars/collapse.jar rescuecore2.LaunchComponents collapse.CollapseSimulator -c $CONFIGDIR/collapse.cfg $* 2>&1 | tee $LOGDIR/collapse-out.log" &
    PIDS="$PIDS $!"
    xterm -T clear -e "java -Xmx256m -cp $CP:$BASEDIR/jars/rescuecore2.jar:$BASEDIR/jars/standard.jar:$BASEDIR/jars/clear.jar rescuecore2.LaunchComponents clear.ClearSimulator -c $CONFIGDIR/clear.cfg $* 2>&1 | tee $LOGDIR/clear-out.log" &
    PIDS="$PIDS $!"

    # Wait for all simulators to start
    waitFor $LOGDIR/misc-out.log "connected"
    waitFor $LOGDIR/traffic-out.log "connected"
    waitFor $LOGDIR/fire-out.log "connected"
    waitFor $LOGDIR/ignition-out.log "connected"
    waitFor $LOGDIR/collapse-out.log "connected"
    waitFor $LOGDIR/clear-out.log "connected"

    xterm -T civilian -e "java -Xmx1024m -cp $CP:$BASEDIR/jars/rescuecore2.jar:$BASEDIR/jars/standard.jar:$BASEDIR/jars/sample.jar:$BASEDIR/jars/kernel.jar rescuecore2.LaunchComponents sample.SampleCivilian*n -c $CONFIGDIR/civilian.cfg $* 2>&1 | tee $LOGDIR/civilian-out.log" &
    PIDS="$PIDS $!"

    # Wait a bit so the civilian XTerm can start up
    sleep 1

    # Viewer
    TEAM_NAME_ARG=""
    if [ ! -z "$TEAM" ]; then
        TEAM_NAME_ARG="\"--viewer.team-name=$TEAM\"";
    fi
    xterm -T viewer -e "java -Xmx256m -cp $CP:$BASEDIR/jars/rescuecore2.jar:$BASEDIR/jars/standard.jar:$BASEDIR/jars/sample.jar rescuecore2.LaunchComponents sample.SampleViewer -c $CONFIGDIR/viewer.cfg $TEAM_NAME_ARG $* 2>&1 | tee $LOGDIR/viewer-out.log" &
    PIDS="$PIDS $!"

    waitFor $LOGDIR/viewer-out.log "connected"
}

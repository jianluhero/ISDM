package rescuecore2;

/**
   Some useful constants that are shared across all parts of the Robocup Rescue software. Note that this does NOT include constants for entity/message types.
 */
public final class Constants {
    /** Config key for message factories. */
    public static final String MESSAGE_FACTORY_KEY = "factory.messages";
    /** Config key for entity factories. */
    public static final String ENTITY_FACTORY_KEY = "factory.entities";
    /** Config key for looking up jars for inspection by a LoadableTypeProcessor. */
    public static final String JAR_DIR_KEY = "loadabletypes.inspect.dir";
    /** Config key for specifying whether to do a deep inspection of jars for loadable types. */
    public static final String DEEP_JAR_INSPECTION_KEY = "loadabletypes.inspect.deep";
    /** Default location for looking up jar files. */
    public static final String DEFAULT_JAR_DIR = "../jars";

}
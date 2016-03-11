package com.tsinghuabigdata.edu.symbolcompute.maxima;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tsinghuabigdata.common.logging.LogFactory;
import com.tsinghuabigdata.common.utils.StringCollectionUtils;
import com.tsinghuabigdata.config.ResourceFinder;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This simple POJO is used to specify how a {@link MaximaProcessLauncher}
 * should run and interact with Maxima.
 * <p/>
 *
 * @author tengyt
 */
public class MaximaConfiguration {
    private static final Logger LOG = LogFactory.getLogger(MaximaConfiguration.class);

    public static final String DEFAULT_CONFIG_FILE = "maxima.properties";

    public static final String EXECUTABLE_KEY = "maxima.executable.path";
    public static final String DEFAULT_TIMEOUT_KEY = "maxima.default.timeout";
    public static final String EXECUTABLE_ARGS_PREFIX_KEY = "maxima.executable.arg";
    public static final String EXECUTABLE_ENV_ARGS_PREFIX_KEY = "maxima.executable.env";

    /**
     * Full path to your Maxima executable file.
     * <p/>
     * This must not be null.
     */
    private String maximaExecutablePath;

    /**
     * Command-line arguments to pass to Maxima executable.
     * <p/>
     * This may be null (or empty) if you don't want to specify anything here.
     */
    private List<String> maximaCommandArguments;

    /**
     * This allows you to pass any required environment variables to Maxima,
     * as described in {@link Runtime#exec(String[], String[])} as <tt>envp</tt>.
     * <p/>
     * This may be null (or empty) if you don't need to specify anything here.
     */
    private Map<String, String> maximaRuntimeEnvironment;

    /**
     * Default time to wait when executing a single call with {@link MaximaInteractiveProcess}
     * before the underlying process gets killed.
     * <p/>
     * Set this to zero to use the default value of {@link MaximaProcessLauncher#DEFAULT_CALL_TIMEOUT}.
     * Set this to a negative number to stop this safety feature from happening.
     */
    private int defaultCallTimeout;


    public MaximaConfiguration() {
    }


    public String getMaximaExecutablePath() {
        return maximaExecutablePath;
    }

    public List<String> getMaximaCommandArguments() {
        return maximaCommandArguments;
    }

    public Map<String, String> getMaximaRuntimeEnvironment() {
        return maximaRuntimeEnvironment;
    }

    public int getDefaultCallTimeout() {
        return defaultCallTimeout;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode())
                + "(maximaExecutablePath=" + maximaExecutablePath
                + ",maximaCommandArguments=" + maximaCommandArguments
                + ",maximaRuntimeEnvironment=" + maximaRuntimeEnvironment
                + ",defaultCallTimeout=" + defaultCallTimeout
                + ")";
    }

    public static MaximaConfiguration defaultConfig() {
        Properties prop = ResourceFinder.buildProperties(DEFAULT_CONFIG_FILE);
        MaximaConfiguration config = new MaximaConfiguration();
        config.maximaExecutablePath = prop.getProperty(EXECUTABLE_KEY, "");
        config.defaultCallTimeout = Integer.parseInt(prop.getProperty(DEFAULT_TIMEOUT_KEY, "10"));
        config.maximaCommandArguments = getIndexedProperty(prop, EXECUTABLE_ARGS_PREFIX_KEY);
        config.maximaRuntimeEnvironment = getEnvs(prop, EXECUTABLE_ENV_ARGS_PREFIX_KEY);
        return config;
    }

    private static List<String> getIndexedProperty(Properties prop, String propertyNameBase) {
        List<String> resultList = Lists.newLinkedList();
        String indexedValue;
        for (int i = 0; ; i++) {
            indexedValue = prop.getProperty(propertyNameBase + i);
            if (indexedValue == null || indexedValue.trim().length() == 0) {
                break;
            }
            resultList.add(indexedValue);
        }
        return resultList;
    }

    private static Map<String, String> getEnvs(Properties prop, String envPrefix) {
        Map<String, String> envs = Maps.newHashMap();

        for (String envPair : getIndexedProperty(prop, envPrefix)) {
            List<String> pair = StringCollectionUtils.split(envPair, "###");
            if (pair.size() != 2) {
                LOG.warn("invalid maxima env config : {}", envPair);
                continue;
            }
            envs.put(pair.get(0), pair.get(1));
        }

        return envs;
    }
}

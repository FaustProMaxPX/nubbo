package icu.nubbo.utils;

public class ServiceUtil {

    public static final String SERVICE_CONCAT_TOKEN = "#";

    public static String makeServiceKey(String interfaceName, String version) {
        String serviceKey = interfaceName;
        if (version != null && !version.trim().isEmpty()) {
            serviceKey += SERVICE_CONCAT_TOKEN + version;
        }
        return serviceKey;
    }
}

package core.common.util;

import org.apache.log4j.Logger;

public class LogUtil {
    private static final Logger logger = Logger.getLogger(LogUtil.class);

    public LogUtil() {
    }

    public static void error(String info, Throwable t) {
        logger.error(info, t);
    }

    public static void error(Throwable t) {
        error("::ERROR::", t);
    }

    public static void error(String error) {
        logger.error(error);
    }

    public static void info(String info) {
        logger.info(info);
    }

    public static void warn(String msg) {
        logger.warn(msg);
    }
}
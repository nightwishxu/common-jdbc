package core.common.util;

/**
 * @param
 * @Auther: xuwenwei
 * @Date: 2018/8/6 11:32
 * @Description:
 */
public class LogFormat {
    public LogFormat() {
    }

    public static void log(String modName, String opName, String oper, String info) {
        log(modName, format(opName, oper, info));
    }

    public static void log(String modName, String opName, String oper, String ip, String info) {
        log(modName, format(opName, oper + " @" + ip, info));
    }

    private static void log(String modName, String info) {
        LogUtil.info("modName:" + modName + " | " + info);
    }

    private static String format(String opName, String oper, String info) {
        StringBuffer str = new StringBuffer(512);
        str.append(opName).append(" [").append(oper).append("] ").append(info.replaceAll("\\r|\\n", ""));
        return str.toString();
    }
}

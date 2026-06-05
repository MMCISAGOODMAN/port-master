package com.portmaster.util;

/**
 * 操作系统类型检测工具
 */
public final class OsDetector {

    public enum OsType {
        WINDOWS, LINUX, MACOS, UNKNOWN
    }

    private static final OsType OS_TYPE;

    static {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            OS_TYPE = OsType.WINDOWS;
        } else if (os.contains("mac") || os.contains("darwin")) {
            OS_TYPE = OsType.MACOS;
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            OS_TYPE = OsType.LINUX;
        } else {
            OS_TYPE = OsType.UNKNOWN;
        }
    }

    private OsDetector() {
    }

    public static OsType getOsType() {
        return OS_TYPE;
    }

    public static boolean isWindows() {
        return OS_TYPE == OsType.WINDOWS;
    }

    public static boolean isLinux() {
        return OS_TYPE == OsType.LINUX;
    }

    public static boolean isMacOS() {
        return OS_TYPE == OsType.MACOS;
    }

    public static boolean isUnixLike() {
        return isLinux() || isMacOS();
    }

    public static String getOsName() {
        return System.getProperty("os.name");
    }
}

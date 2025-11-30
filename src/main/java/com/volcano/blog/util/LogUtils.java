package com.volcano.blog.util;

/**
 * 日志工具类
 * 提供敏感信息脱敏功能
 */
public final class LogUtils {

    private LogUtils() {
        // 工具类不允许实例化
    }

    /**
     * 邮箱脱敏
     * 例如: test@example.com -> t***@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "[empty]";
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return maskString(email);
        }
        
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (localPart.length() <= 1) {
            return "*" + domain;
        }
        
        return localPart.charAt(0) + "***" + domain;
    }

    /**
     * IP 地址脱敏
     * 例如: 192.168.1.100 -> 192.168.*.*
     */
    public static String maskIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "[empty]";
        }
        
        // IPv4
        if (ip.contains(".")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".*.*";
            }
        }
        
        // IPv6 或其他格式，只显示前几个字符
        if (ip.length() > 8) {
            return ip.substring(0, 8) + "***";
        }
        
        return maskString(ip);
    }

    /**
     * 通用字符串脱敏
     * 保留首尾字符，中间用 * 替代
     */
    public static String maskString(String str) {
        if (str == null || str.isEmpty()) {
            return "[empty]";
        }
        
        int len = str.length();
        if (len <= 2) {
            return "*".repeat(len);
        }
        
        return str.charAt(0) + "*".repeat(len - 2) + str.charAt(len - 1);
    }

    /**
     * 用户 ID 脱敏（可选，通常 ID 不需要脱敏）
     */
    public static String maskUserId(Long userId) {
        return userId != null ? userId.toString() : "[null]";
    }
}

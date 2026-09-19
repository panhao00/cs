package com.crs.security;

/**
 * 数据脱敏工具类: 用于敏感信息的显示脱敏处理。
 * <p>
 * 对应需求第 10 节安全与审计要求:
 * - "student grades are sensitive information, the system must employ extra security measures"
 * <p>
 * 脱敏规则:
 * - SSN: "***-**-1234" 格式,仅显示最后 4 位
 * - 姓名: "张*三" 或 "Charlie B." 格式,仅显示首尾字符
 * <p>
 * 使用 Java 内置功能,无需外部加密库。
 */
public final class DataMaskUtil {

    private DataMaskUtil() {
        // 工具类禁止实例化
    }

    /**
     * SSN 脱敏处理。
     * <p>
     * 将完整 SSN (如 "123-45-6789") 转换为脱敏格式 ("***-**-1234")。
     * 若 SSN 为 null 或长度不足 4 位,则返回原值。
     *
     * @param ssn 完整 SSN
     * @return 脱敏后的 SSN
     */
    public static String maskSSN(String ssn) {
        if (ssn == null || ssn.isEmpty()) {
            return ssn;
        }
        // 移除可能的格式化字符 (连字符等)
        String cleanSSN = ssn.replaceAll("[^0-9]", "");
        if (cleanSSN.length() < 4) {
            return ssn; // 长度不足,返回原值
        }
        // 保留最后 4 位,其余用 * 替代
        String lastFour = cleanSSN.substring(cleanSSN.length() - 4);
        return "***-**-" + lastFour;
    }

    /**
     * 姓名脱敏处理。
     * <p>
     * 中文姓名: "张三" → "张*", "张三丰" → "张*丰"
     * 英文姓名: "Charlie Brown" → "Charlie B.", "Bob" → "B."
     *
     * @param name 完整姓名
     * @return 脱敏后的姓名
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        // 检测是否包含中文字符
        if (name.chars().anyMatch(Character::isIdeographic)) {
            return maskChineseName(name);
        } else {
            return maskEnglishName(name);
        }
    }

    /**
     * 中文姓名脱敏。
     * <p>
     * 2 字: "张三" → "张*"
     * 3+ 字: "张三丰" → "张*丰"
     *
     * @param name 中文姓名
     * @return 脱敏后的姓名
     */
    private static String maskChineseName(String name) {
        if (name.length() == 1) {
            return "*"; // 单字名
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*"; // 两字名: 保留姓
        }
        // 三字及以上: 保留姓和名的最后一个字
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }

    /**
     * 英文姓名脱敏。
     * <p>
     * 单名: "Charlie" → "C."
     * 多名: "Charlie Brown" → "Charlie B.", "Charlie Benjamin Brown" → "Charlie B."
     *
     * @param name 英文姓名
     * @return 脱敏后的姓名
     */
    private static String maskEnglishName(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0) {
            return name;
        }

        StringBuilder sb = new StringBuilder();
        // 第一个名字完整保留
        sb.append(parts[0]);

        // 后续名字只保留首字母 + "."
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(" ");
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
                sb.append(".");
            }
        }

        return sb.toString();
    }
}

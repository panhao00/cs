package com.crs.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.SecretKey;
import java.util.Base64;

/**
 * 成绩加密转换器: 将明文成绩加密后存储到数据库。
 * <p>
 * 密钥优先级:
 * 1. 系统属性 crs.grade.encryption.key
 * 2. 环境变量 GRADE_ENCRYPTION_KEY
 * 3. 开发环境自动生成(仅限非 prod profile)
 * <p>
 * 生产环境(spring.profiles.active 含 prod)必须显式配置密钥,否则启动即失败,
 * 避免多实例/重启导致各自生成随机密钥、历史成绩无法解密。
 */
@Converter
public class GradeEncryptionConverter implements AttributeConverter<String, String> {

    private static final SecretKey KEY;

    static {
        String keyBase64 = System.getProperty("crs.grade.encryption.key");
        if (keyBase64 == null || keyBase64.isBlank()) {
            keyBase64 = System.getenv("GRADE_ENCRYPTION_KEY");
        }
        if (keyBase64 == null || keyBase64.isBlank()) {
            if (isProdProfile()) {
                throw new IllegalStateException(
                        "生产环境必须配置成绩加密密钥 GRADE_ENCRYPTION_KEY (32字节 Base64)。"
                        + "可用命令生成: openssl rand -base64 32。"
                        + "缺失时每个实例/重启会生成随机密钥,导致历史加密成绩无法解密。");
            }
            // 开发环境自动生成
            keyBase64 = Base64.getEncoder().encodeToString(
                    EncryptionUtil.generateKey().getEncoded());
        }
        KEY = EncryptionUtil.getKeyFromBase64(keyBase64);
    }

    /**
     * 判断当前是否为生产环境。同时检查系统属性与环境变量,覆盖 IDE/docker 两种启动方式。
     */
    private static boolean isProdProfile() {
        String prop = System.getProperty("spring.profiles.active");
        String env = System.getenv("SPRING_PROFILES_ACTIVE");
        String profiles = (prop != null && !prop.isBlank()) ? prop : env;
        return profiles != null && profiles.contains("prod");
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return EncryptionUtil.encrypt(attribute, KEY);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        // Check if data is already encrypted (backward compatibility)
        if (dbData.matches("^[A-Za-z0-9+/=]{20,}$") && dbData.length() > 10) {
            try {
                return EncryptionUtil.decrypt(dbData, KEY);
            } catch (Exception e) {
                // Not encrypted, return as-is (backward compatibility)
                return dbData;
            }
        }
        return dbData;
    }
}

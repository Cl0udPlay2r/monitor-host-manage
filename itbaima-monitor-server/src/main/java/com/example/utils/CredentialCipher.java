package com.example.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 凭据对称加密工具。
 * <p>
 * 原实现把 SSH 口令以明文写入 {@code db_client_ssh.password}，数据库或备份一旦泄漏即导致
 * 所有被管主机失守（见测试记录 F4）。
 * 这里改用 AES/GCM 加密后以 Base64 存储，并加 {@code enc:} 前缀作为密文标记；
 * 读取时若没有该前缀，则按历史明文处理（保证存量数据仍然可用），同时打告警提醒迁移。
 */
@Slf4j
@Component
public class CredentialCipher {

    private static final String PREFIX = "enc:";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public CredentialCipher(@Value("${spring.security.credential.key}") String secret) throws Exception {
        // 用 SHA-256 把任意长度的口令派生成 128 位 AES 密钥，避免要求配置里必须写满 16 字节
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        this.key = new SecretKeySpec(digest, "AES");
    }

    /** 加密；已加密的值原样返回，避免重复加密 */
    public String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) return plain;
        if (isEncrypted(plain)) return plain;
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("凭据加密失败", e);
        }
    }

    /** 解密；历史明文原样返回以便平滑迁移 */
    public String decrypt(String stored) {
        if (stored == null || stored.isEmpty()) return stored;
        if (!isEncrypted(stored)) {
            log.warn("检测到未加密的历史凭据，请重新保存一次以完成加密迁移");
            return stored;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, combined, 0, IV_LENGTH));
            return new String(cipher.doFinal(combined, IV_LENGTH, combined.length - IV_LENGTH),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("凭据解密失败，可能是加密密钥已变更", e);
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }
}

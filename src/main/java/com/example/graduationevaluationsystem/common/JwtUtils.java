package com.example.graduationevaluationsystem.common;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 */
public class JwtUtils {

    private static final String SECRET = "graduation-evaluation-system-2026";

    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000L;

    /**
     * 生成 JWT Token
     *
     * @param userType 用户类型（admin/teacher/student）
     * @param username 账号
     * @param name     姓名
     * @return JWT Token
     */
    public static String generateToken(String userType, String username, String name) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(JWTPayload.SUBJECT, username);
        payload.put("userType", userType);
        payload.put("username", username);
        payload.put("name", name);
        payload.put(JWTPayload.ISSUED_AT, System.currentTimeMillis() / 1000);
        payload.put(JWTPayload.EXPIRES_AT, (System.currentTimeMillis() + EXPIRATION_MS) / 1000);
        return JWTUtil.createToken(payload, SECRET.getBytes());
    }

    /**
     * 解析并验证 Token
     *
     * @param token JWT Token
     * @return 解析后的 Payload，验证失败返回 null
     */
    public static JWTPayload verify(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            if (!jwt.setKey(SECRET.getBytes()).verify()) {
                return null;
            }
            // 校验是否过期
            Object expObj = jwt.getPayload().getClaim(JWTPayload.EXPIRES_AT);
            if (expObj != null) {
                long exp;
                if (expObj instanceof Number) {
                    exp = ((Number) expObj).longValue();
                } else {
                    exp = Long.parseLong(expObj.toString());
                }
                // exp 是秒级时间戳
                if (exp * 1000 < System.currentTimeMillis()) {
                    return null;
                }
            }
            return jwt.getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 Token 中获取用户类型
     */
    public static String getUserType(String token) {
        JWTPayload payload = verify(token);
        return payload != null ? (String) payload.getClaim("userType") : null;
    }

    /**
     * 从 Token 中获取用户账号
     */
    public static String getUsername(String token) {
        JWTPayload payload = verify(token);
        return payload != null ? (String) payload.getClaim("username") : null;
    }

    /**
     * 从 Token 中获取用户姓名
     */
    public static String getName(String token) {
        JWTPayload payload = verify(token);
        return payload != null ? (String) payload.getClaim("name") : null;
    }
}

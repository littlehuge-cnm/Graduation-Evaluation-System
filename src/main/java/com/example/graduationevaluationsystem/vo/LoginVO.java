package com.example.graduationevaluationsystem.vo;

import lombok.Data;

/**
 * 登录响应 VO
 */
@Data
public class LoginVO {

    private String token;
    private String userType;
    private String username;
    private String name;
}

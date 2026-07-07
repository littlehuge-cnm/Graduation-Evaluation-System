package com.example.graduationevaluationsystem.vo;

import lombok.Data;

import java.util.List;

/**
 * 当前登录用户信息 VO
 */
@Data
public class UserInfoVO {

    private String userType;
    private String username;
    private String name;
    private List<String> identities;
}

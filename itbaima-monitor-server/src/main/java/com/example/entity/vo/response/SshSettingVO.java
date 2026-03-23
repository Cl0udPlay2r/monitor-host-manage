package com.example.entity.vo.response;

import lombok.Data;

@Data
public class SshSettingVO {
    private String ip;
    private int port = 22;
    private String username;
    private String password;
}

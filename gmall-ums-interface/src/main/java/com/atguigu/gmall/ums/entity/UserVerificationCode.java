package com.atguigu.gmall.ums.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
@NoArgsConstructor
public class UserVerificationCode {
    private String phone;
    private String VerificationCode;
}

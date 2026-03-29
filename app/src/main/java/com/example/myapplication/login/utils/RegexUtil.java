package com.example.myapplication.login.utils;

public class RegexUtil {
    public static boolean isPhoneValid(String phone) {
        if (phone == null) {
            return false;
        }
        // Java 正则写法：注意是 两个反斜杠 \\d
        return phone.matches("^1[3-9]\\d{9}$");
    }
    public static boolean isPasswordValid(String password) {
        if (password == null) {
            return false;
        }
        // Java 里要写成 \\d 而不是 \d
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,20}$");
    }
}

package com.superme.util;

import com.superme.model.User;

import java.security.SecureRandom;
import java.util.Random;

public class ReferralCodeGenerator {
    //Referral Code


    public static String randomAlphaNumeric(int len) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random rnd = new SecureRandom();
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
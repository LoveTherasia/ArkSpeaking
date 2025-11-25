package com.organization.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5Util {
    public static String code(String str){
        try{
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            byte[]byteDigest = md.digest();
            int i;
            StringBuilder buf = new StringBuilder("");

            for(int offset = 0; offset < byteDigest.length; offset++){
                i = byteDigest[offset];
                if(i<0){
                    i+=256;
                }
                if(i<16){
                    buf.append("0");
                }
                buf.append(Integer.toHexString(i));
            }
            return buf.toString();
        }   catch(NoSuchAlgorithmException e){
            DebugLogger.logException("md5加密失败",e);
            return null;
        }
    }
}

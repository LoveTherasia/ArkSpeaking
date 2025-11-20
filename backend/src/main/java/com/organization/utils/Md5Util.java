package com.organization.utils;

import java.io.NotActiveException;
import java.lang.invoke.SerializedLambda;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5Util {
    public static String code(String str){
        try{
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            byte[]byteDigest = md.digest();
            int i;
            StringBuffer buf = new StringBuffer("");

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
            e.printStackTrace();
            return null;
        }
    }
}

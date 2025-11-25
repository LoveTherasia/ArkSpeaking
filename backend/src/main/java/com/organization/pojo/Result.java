package com.organization.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//响应数据类
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    //快速返回操作成功并返回非空数据
    public static <E> Result<E> success(E data){
        return new Result<>(0,"操作成功",data);
    }

    //快速返回操作成功并返回空数据
    public static Result<Void> success(){
        return new Result<>(0,"操作成功",null);
    }

    //返回操作失败
    public static Result<Void> error(String message){
        return new Result<>(1,message,null);
    }
}

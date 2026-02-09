package com.organization.mapper;

import com.organization.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {
    @Select("SELECT id,nickname,avatar,signature FROM `user` WHERE id = 1")
    User getCurrentUser();

    @Update("UPDATE `user` SET nickname = #{nickname},avatar = #{avatar},signature = #{signature} WHERE id = 1")
    int updateUserInfo(User user);
}

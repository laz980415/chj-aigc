package com.chj.aigc.authservice.persistence.mapper;

import com.chj.aigc.authservice.auth.AuthUser;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 认证服务账号与会话 Mapper。
 */
public interface AuthMapper {
    Map<String, Object> findUserByUsername(@Param("username") String username);

    Map<String, Object> findSessionByToken(@Param("token") String token);

    void upsertSession(Map<String, Object> session);
}

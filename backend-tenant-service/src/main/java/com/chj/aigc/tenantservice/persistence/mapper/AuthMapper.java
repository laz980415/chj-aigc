package com.chj.aigc.tenantservice.persistence.mapper;

import com.chj.aigc.tenantservice.auth.AuthUser;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 账号与会话 MyBatis 映射接口。
 */
public interface AuthMapper {
    List<Map<String, Object>> listUsers();

    Map<String, Object> findUserById(@Param("userId") String userId);

    Map<String, Object> findUserByUsername(@Param("username") String username);

    void upsertUser(AuthUser user);

    Map<String, Object> findSessionByToken(@Param("token") String token);

    void upsertSession(Map<String, Object> session);
}

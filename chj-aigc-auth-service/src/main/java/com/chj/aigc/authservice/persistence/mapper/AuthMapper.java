package com.chj.aigc.authservice.persistence.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 认证服务账号与会话 Mapper。
 */
public interface AuthMapper {
    Map<String, Object> findUserByUsername(@Param("username") String username);

    Map<String, Object> findUserById(@Param("id") String id);

    List<Map<String, Object>> listUsers();

    List<Map<String, Object>> listUsersByTenantId(@Param("tenantId") String tenantId);

    void upsertUser(Map<String, Object> user);

    Map<String, Object> findSessionByToken(@Param("token") String token);

    void upsertSession(Map<String, Object> session);
}

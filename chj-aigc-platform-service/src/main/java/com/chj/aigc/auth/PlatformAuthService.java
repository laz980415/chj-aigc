package com.chj.aigc.auth;

import java.util.List;
import java.util.Optional;

/**
 * 平台服务账号与会话能力接口。
 * 本地实现直接查库，远程实现通过 HTTP 调用认证服务。
 */
public interface PlatformAuthService {
    Optional<AuthSession> findSession(String token);

    List<AuthUser> listUsers();

    List<AuthUser> listTenantUsers(String tenantId);

    List<String> builtinRoles();

    AuthUser createUser(String userId, String username, String password,
                        String displayName, String roleKey, String tenantId);
}

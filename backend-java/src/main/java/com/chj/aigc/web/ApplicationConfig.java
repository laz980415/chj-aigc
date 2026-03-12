package com.chj.aigc.web;

import com.chj.aigc.auth.AuthInterceptor;
import com.chj.aigc.auth.AuthService;
import com.chj.aigc.auth.AuthStore;
import com.chj.aigc.auth.InMemoryAuthStore;
import com.chj.aigc.auth.JdbcAuthStore;
import com.chj.aigc.access.ModelAccessPolicyEngine;
import com.chj.aigc.access.ModelAccessAdminStore;
import com.chj.aigc.access.InMemoryModelAccessAdminStore;
import com.chj.aigc.access.JdbcModelAccessAdminStore;
import com.chj.aigc.billing.InMemoryTenantBillingStore;
import com.chj.aigc.billing.JdbcTenantBillingStore;
import com.chj.aigc.billing.TenantBillingService;
import com.chj.aigc.billing.TenantBillingStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApplicationConfig {
    @Bean
    public ModelAccessPolicyEngine modelAccessPolicyEngine() {
        return new ModelAccessPolicyEngine();
    }

    @Bean
    public ModelAccessAdminStore modelAccessAdminStore(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate != null) {
            return new JdbcModelAccessAdminStore(jdbcTemplate);
        }
        return new InMemoryModelAccessAdminStore();
    }

    @Bean
    public AuthStore authStore(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate != null) {
            return new JdbcAuthStore(jdbcTemplate);
        }
        return new InMemoryAuthStore();
    }

    @Bean
    public AuthService authService(AuthStore authStore) {
        return new AuthService(authStore);
    }

    @Bean
    public TenantBillingStore tenantBillingStore(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate != null) {
            return new JdbcTenantBillingStore(jdbcTemplate);
        }
        return new InMemoryTenantBillingStore();
    }

    @Bean
    public TenantBillingService tenantBillingService(TenantBillingStore tenantBillingStore) {
        return new TenantBillingService(tenantBillingStore);
    }

    @Bean
    public AuthInterceptor authInterceptor(AuthService authService) {
        return new AuthInterceptor(authService);
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer(AuthInterceptor authInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(
                                "http://127.0.0.1:5173",
                                "http://localhost:5173"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }

            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(authInterceptor)
                        .addPathPatterns("/api/**");
            }
        };
    }
}

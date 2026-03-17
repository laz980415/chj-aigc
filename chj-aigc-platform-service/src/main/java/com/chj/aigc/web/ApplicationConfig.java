package com.chj.aigc.web;

import com.chj.aigc.auth.AuthInterceptor;
import com.chj.aigc.auth.AuthService;
import com.chj.aigc.auth.AuthStore;
import com.chj.aigc.auth.InMemoryAuthStore;
import com.chj.aigc.auth.MybatisAuthStore;
import com.chj.aigc.auth.PlatformAuthService;
import com.chj.aigc.auth.RemoteAuthService;
import com.chj.aigc.access.ModelAccessPolicyEngine;
import com.chj.aigc.access.ModelAccessAdminStore;
import com.chj.aigc.access.InMemoryModelAccessAdminStore;
import com.chj.aigc.access.MybatisModelAccessAdminStore;
import com.chj.aigc.billing.InMemoryTenantBillingStore;
import com.chj.aigc.billing.MybatisTenantBillingStore;
import com.chj.aigc.billing.TenantBillingService;
import com.chj.aigc.billing.TenantBillingStore;
import com.chj.aigc.persistence.mapper.AuthMapper;
import com.chj.aigc.persistence.mapper.ModelAccessMapper;
import com.chj.aigc.persistence.mapper.TenantBillingMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApplicationConfig {
    /**
     * 统一装配平台服务需要的 Web、鉴权和平台管理组件。
     * 租户工作台能力已经迁移到独立的 backend-tenant-service。
     * 当前平台服务目录名为 chj-aigc-platform-service。
     */
    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dataSource(
            @Value("${spring.datasource.url:jdbc:postgresql://36.150.108.207:54312/chj-aigc}") String url,
            @Value("${spring.datasource.username:postgres}") String username,
            @Value("${spring.datasource.password:${APP_DB_PASSWORD:}}") String password,
            @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}") String driverClassName
    ) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        return dataSource;
    }

    @Bean
    public DataSourceInitializer dataSourceInitializer(DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("schema.sql"));
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(populator);
        return initializer;
    }

    @Bean
    @DependsOn("dataSourceInitializer")
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/*.xml"));
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);
        return factoryBean.getObject();
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean
    public ModelAccessPolicyEngine modelAccessPolicyEngine() {
        return new ModelAccessPolicyEngine();
    }

    @Bean
    public ModelAccessAdminStore modelAccessAdminStore(ObjectProvider<ModelAccessMapper> mapperProvider) {
        ModelAccessMapper mapper = mapperProvider.getIfAvailable();
        if (mapper != null) {
            return new MybatisModelAccessAdminStore(mapper);
        }
        return new InMemoryModelAccessAdminStore();
    }

    @Bean
    public PlatformAuthService authService(
            @Value("${auth.service-uri:}") String authServiceUri,
            ObjectProvider<AuthMapper> mapperProvider
    ) {
        // 优先使用远程认证服务，认证服务是 auth 表的唯一拥有者
        if (authServiceUri != null && !authServiceUri.isBlank()) {
            return new RemoteAuthService(authServiceUri);
        }
        // 降级：本地 MyBatis 或内存存储（仅用于单机开发）
        AuthMapper mapper = mapperProvider.getIfAvailable();
        AuthStore store = mapper != null ? new MybatisAuthStore(mapper) : new InMemoryAuthStore();
        return new AuthService(store);
    }

    @Bean
    public TenantBillingStore tenantBillingStore(ObjectProvider<TenantBillingMapper> mapperProvider) {
        TenantBillingMapper mapper = mapperProvider.getIfAvailable();
        if (mapper != null) {
            return new MybatisTenantBillingStore(mapper);
        }
        return new InMemoryTenantBillingStore();
    }

    @Bean
    public TenantBillingService tenantBillingService(TenantBillingStore tenantBillingStore) {
        return new TenantBillingService(tenantBillingStore);
    }

    @Bean
    public AuthInterceptor authInterceptor(PlatformAuthService authService) {
        return new AuthInterceptor(authService);
    }

    @Bean
    public FilterRegistrationBean<TraceContextFilter> traceContextFilterRegistration() {
        FilterRegistrationBean<TraceContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceContextFilter());
        registration.setOrder(Integer.MIN_VALUE);
        registration.addUrlPatterns("/*");
        return registration;
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

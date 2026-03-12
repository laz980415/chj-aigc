package com.chj.aigc.tenantservice.web;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.chj.aigc.tenantservice.asset.AssetCatalogStore;
import com.chj.aigc.tenantservice.asset.MybatisAssetCatalogStore;
import com.chj.aigc.tenantservice.asset.TenantAssetCatalogService;
import com.chj.aigc.tenantservice.auth.AuthInterceptor;
import com.chj.aigc.tenantservice.auth.AuthService;
import com.chj.aigc.tenantservice.auth.AuthStore;
import com.chj.aigc.tenantservice.auth.MybatisAuthStore;
import com.chj.aigc.tenantservice.billing.MybatisTenantBillingStore;
import com.chj.aigc.tenantservice.billing.TenantBillingService;
import com.chj.aigc.tenantservice.billing.TenantBillingStore;
import com.chj.aigc.tenantservice.persistence.mapper.AssetCatalogMapper;
import com.chj.aigc.tenantservice.persistence.mapper.AuthMapper;
import com.chj.aigc.tenantservice.persistence.mapper.TenantBillingMapper;
import com.chj.aigc.tenantservice.persistence.mapper.TenantProjectMapper;
import com.chj.aigc.tenantservice.tenant.MybatisTenantProjectStore;
import com.chj.aigc.tenantservice.tenant.TenantProjectStore;
import com.chj.aigc.tenantservice.tenant.TenantWorkspaceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 租户服务组件装配。
 */
@Configuration
public class ApplicationConfig {
    @Bean
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.driver-class-name}") String driverClassName
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
    public AuthStore authStore(AuthMapper authMapper) {
        return new MybatisAuthStore(authMapper);
    }

    @Bean
    public AuthService authService(AuthStore authStore) {
        return new AuthService(authStore);
    }

    @Bean
    public AssetCatalogStore assetCatalogStore(AssetCatalogMapper assetCatalogMapper) {
        return new MybatisAssetCatalogStore(assetCatalogMapper);
    }

    @Bean
    public TenantAssetCatalogService tenantAssetCatalogService(AssetCatalogStore assetCatalogStore) {
        return new TenantAssetCatalogService(assetCatalogStore);
    }

    @Bean
    public TenantProjectStore tenantProjectStore(TenantProjectMapper tenantProjectMapper) {
        return new MybatisTenantProjectStore(tenantProjectMapper);
    }

    @Bean
    public TenantWorkspaceService tenantWorkspaceService(TenantProjectStore tenantProjectStore, AuthService authService) {
        return new TenantWorkspaceService(tenantProjectStore, authService);
    }

    @Bean
    public TenantBillingStore tenantBillingStore(TenantBillingMapper tenantBillingMapper) {
        return new MybatisTenantBillingStore(tenantBillingMapper);
    }

    @Bean
    public TenantBillingService tenantBillingService(TenantBillingStore tenantBillingStore) {
        return new TenantBillingService(tenantBillingStore);
    }

    @Bean
    public AuthInterceptor authInterceptor(AuthService authService, ObjectMapper objectMapper) {
        return new AuthInterceptor(authService, objectMapper);
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer(AuthInterceptor authInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://127.0.0.1:5173", "http://localhost:5173")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }

            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
            }
        };
    }
}

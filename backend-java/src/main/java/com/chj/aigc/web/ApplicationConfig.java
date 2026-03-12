package com.chj.aigc.web;

import com.chj.aigc.auth.AuthInterceptor;
import com.chj.aigc.auth.AuthService;
import com.chj.aigc.auth.AuthStore;
import com.chj.aigc.auth.InMemoryAuthStore;
import com.chj.aigc.auth.MybatisAuthStore;
import com.chj.aigc.access.ModelAccessPolicyEngine;
import com.chj.aigc.access.ModelAccessAdminStore;
import com.chj.aigc.access.InMemoryModelAccessAdminStore;
import com.chj.aigc.access.MybatisModelAccessAdminStore;
import com.chj.aigc.billing.InMemoryTenantBillingStore;
import com.chj.aigc.billing.MybatisTenantBillingStore;
import com.chj.aigc.billing.TenantBillingService;
import com.chj.aigc.billing.TenantBillingStore;
import com.chj.aigc.asset.AssetCatalogStore;
import com.chj.aigc.asset.InMemoryAssetCatalogStore;
import com.chj.aigc.asset.MybatisAssetCatalogStore;
import com.chj.aigc.asset.TenantAssetCatalogService;
import com.chj.aigc.tenant.InMemoryTenantProjectStore;
import com.chj.aigc.tenant.MybatisTenantProjectStore;
import com.chj.aigc.tenant.TenantProjectStore;
import com.chj.aigc.tenant.TenantWorkspaceService;
import com.chj.aigc.persistence.mapper.AssetCatalogMapper;
import com.chj.aigc.persistence.mapper.AuthMapper;
import com.chj.aigc.persistence.mapper.ModelAccessMapper;
import com.chj.aigc.persistence.mapper.TenantBillingMapper;
import com.chj.aigc.persistence.mapper.TenantProjectMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.ObjectProvider;
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
     * 统一装配 Web、鉴权、存储和租户工作台相关组件。
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
    public AuthStore authStore(ObjectProvider<AuthMapper> mapperProvider) {
        AuthMapper mapper = mapperProvider.getIfAvailable();
        if (mapper != null) {
            return new MybatisAuthStore(mapper);
        }
        return new InMemoryAuthStore();
    }

    @Bean
    public AuthService authService(AuthStore authStore) {
        return new AuthService(authStore);
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
    public AssetCatalogStore assetCatalogStore(ObjectProvider<AssetCatalogMapper> mapperProvider) {
        AssetCatalogMapper mapper = mapperProvider.getIfAvailable();
        if (mapper != null) {
            return new MybatisAssetCatalogStore(mapper);
        }
        return new InMemoryAssetCatalogStore();
    }

    @Bean
    public TenantAssetCatalogService tenantAssetCatalogService(AssetCatalogStore assetCatalogStore) {
        return new TenantAssetCatalogService(assetCatalogStore);
    }

    @Bean
    public TenantProjectStore tenantProjectStore(ObjectProvider<TenantProjectMapper> mapperProvider) {
        TenantProjectMapper mapper = mapperProvider.getIfAvailable();
        if (mapper != null) {
            return new MybatisTenantProjectStore(mapper);
        }
        return new InMemoryTenantProjectStore();
    }

    @Bean
    public TenantWorkspaceService tenantWorkspaceService(TenantProjectStore tenantProjectStore, AuthService authService) {
        return new TenantWorkspaceService(tenantProjectStore, authService);
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

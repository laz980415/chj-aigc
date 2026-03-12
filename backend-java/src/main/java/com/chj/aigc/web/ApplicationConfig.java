package com.chj.aigc.web;

import com.chj.aigc.access.ModelAccessPolicyEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
    @Bean
    public ModelAccessPolicyEngine modelAccessPolicyEngine() {
        return new ModelAccessPolicyEngine();
    }
}

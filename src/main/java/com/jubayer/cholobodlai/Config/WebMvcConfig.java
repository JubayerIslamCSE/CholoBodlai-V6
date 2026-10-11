package com.jubayer.cholobodlai.Config;

import com.jubayer.cholobodlai.Security.ActiveSessionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final ActiveSessionInterceptor sessions;
    public WebMvcConfig(ActiveSessionInterceptor sessions) { this.sessions = sessions; }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessions).addPathPatterns("/shipper/**", "/owner/**", "/admin/**",
                "/trips/**", "/trucks/**", "/profile/**", "/notifications/**", "/history/**");
    }
}

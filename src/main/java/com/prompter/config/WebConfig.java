package com.prompter.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(
                    jakarta.servlet.http.HttpServletRequest request,
                    jakarta.servlet.http.HttpServletResponse response,
                    Object handler) throws Exception {

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated()
                        && !"anonymousUser".equals(auth.getPrincipal())) {
                    String name;
                    Object principal = auth.getPrincipal();
                    if (principal instanceof org.springframework.security.core.userdetails.User u) {
                        name = u.getUsername();
                    } else {
                        name = principal.toString();
                    }
                    request.setAttribute("currentUser", name);
                }
                return true;
            }
        }).addPathPatterns("/**");
    }
}

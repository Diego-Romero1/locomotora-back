package com.locomotora.demo.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * ETag para el catalogo de ejercicios: es grande (~900 items) y casi nunca cambia. Si el cliente
 * manda If-None-Match con el mismo hash, responde 304 sin cuerpo.
 */
@Configuration
public class HttpCacheConfig {
    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> exerciseCatalogEtagFilter() {
        ShallowEtagHeaderFilter filter = new ShallowEtagHeaderFilter();
        // Debil (W/"..."): Tomcat no comprime con gzip las respuestas con un ETag fuerte.
        filter.setWriteWeakETag(true);
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/exercises");
        registration.setName("exerciseCatalogEtagFilter");
        return registration;
    }
}

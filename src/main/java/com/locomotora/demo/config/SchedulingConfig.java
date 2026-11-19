package com.locomotora.demo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Procesos programados (llenado y refresco de videos del catalogo). Se apagan con locomotora.jobs.enabled=false. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "locomotora.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}

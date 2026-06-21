package com.fraudwatch.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fraudwatch.observability.http-correlation")
public class HttpCorrelationProperties {

    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

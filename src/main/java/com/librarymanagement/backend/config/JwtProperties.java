package com.librarymanagement.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JwtProperties - binds JWT configuration from application.properties.
 *
 * @ConfigurationProperties(prefix = "app.jwt") tells Spring to look for properties
 * that start with "app.jwt" and bind them to the fields of this class.
 *
 *   app.jwt.secret          -> maps to the 'secret' field
 *   app.jwt.expiration-ms   -> maps to the 'expirationMs' field
 *
 * These values come from environment variables (see application.properties):
 *   app.jwt.secret = ${JWT_SECRET}
 *   app.jwt.expiration-ms = 86400000
 *
 * This class is used in Phase 4 (Security) for JWT generation and validation.
 * Defined here in Phase 3 so the app compiles with the existing application.properties.
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * The secret key used to sign JWT tokens.
     * Loaded from the JWT_SECRET environment variable via application.properties.
     * Must be at least 256 bits (32 characters) for HMAC-SHA256.
     * NEVER log, print, or expose this value.
     */
    private String secret;

    /**
     * Token expiration time in milliseconds.
     * Default: 86400000 ms = 24 hours.
     */
    private long expirationMs;

    // Getters and setters required by @ConfigurationProperties binding

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getExpirationMs() { return expirationMs; }
    public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }
}

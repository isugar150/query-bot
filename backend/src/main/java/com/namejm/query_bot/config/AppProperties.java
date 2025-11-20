package com.namejm.query_bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Security security = new Security();
    private final OpenAi openai = new OpenAi();

    public Security getSecurity() {
        return security;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    public static class Security {
        /**
         * Secret used to sign JWTs. Override in production using the JWT_SECRET environment variable.
         */
        private String jwtSecret;
        private int accessTokenMinutes = 30;
        private int refreshTokenDays = 7;

        public String getJwtSecret() {
            return jwtSecret;
        }

        public void setJwtSecret(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        public int getAccessTokenMinutes() {
            return accessTokenMinutes;
        }

        public void setAccessTokenMinutes(int accessTokenMinutes) {
            this.accessTokenMinutes = accessTokenMinutes;
        }

        public int getRefreshTokenDays() {
            return refreshTokenDays;
        }

        public void setRefreshTokenDays(int refreshTokenDays) {
            this.refreshTokenDays = refreshTokenDays;
        }
    }

    public static class OpenAi {
        private String apiKey;
        private String model;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }
}

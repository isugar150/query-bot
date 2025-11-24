package com.namejm.query_bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Security security = new Security();
    private final OpenAi openai = new OpenAi();
    private final Metabase metabase = new Metabase();
    private String dataDir = "./data";

    public Security getSecurity() {
        return security;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    public Metabase getMetabase() {
        return metabase;
    }

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
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

    public static class Metabase {
        private String url;
        private String apiKey;
        private Long databaseKey;
        private Long collectionKey;
        private String deviceId;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public Long getDatabaseKey() {
            return databaseKey;
        }

        public void setDatabaseKey(Long databaseKey) {
            this.databaseKey = databaseKey;
        }

        public Long getCollectionKey() {
            return collectionKey;
        }

        public void setCollectionKey(Long collectionKey) {
            this.collectionKey = collectionKey;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }
    }
}

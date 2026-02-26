package com.arsc.bookchaingateway.trace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "fabric")
public class FabricProperties {

    private String channelName;
    private String chaincodeName;
    private int timeoutSeconds = 30;
    private Map<String, OrgConfig> organizations = new HashMap<>();

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChaincodeName() {
        return chaincodeName;
    }

    public void setChaincodeName(String chaincodeName) {
        this.chaincodeName = chaincodeName;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Map<String, OrgConfig> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(Map<String, OrgConfig> organizations) {
        this.organizations = organizations;
    }

    public OrgConfig getOrgConfig(String orgId) {
        String key = orgId.toLowerCase();
        if (!organizations.containsKey(key)) {
            throw new IllegalArgumentException("未找到组织配置: " + orgId);
        }
        return organizations.get(key);
    }

    public static class OrgConfig {
        private String mspId;
        private String peerEndpoint;
        private String overrideAuth;
        private String tlsCert;
        private String userCert;
        private String userKey;

        public String getMspId() {
            return mspId;
        }

        public void setMspId(String mspId) {
            this.mspId = mspId;
        }

        public String getPeerEndpoint() {
            return peerEndpoint;
        }

        public void setPeerEndpoint(String peerEndpoint) {
            this.peerEndpoint = peerEndpoint;
        }

        public String getOverrideAuth() {
            return overrideAuth;
        }

        public void setOverrideAuth(String overrideAuth) {
            this.overrideAuth = overrideAuth;
        }

        public String getTlsCert() {
            return tlsCert;
        }

        public void setTlsCert(String tlsCert) {
            this.tlsCert = tlsCert;
        }

        public String getUserCert() {
            return userCert;
        }

        public void setUserCert(String userCert) {
            this.userCert = userCert;
        }

        public String getUserKey() {
            return userKey;
        }

        public void setUserKey(String userKey) {
            this.userKey = userKey;
        }
    }
}

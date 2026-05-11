package com.wisewallet.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "gateway")
@Getter
@Setter
public class GatewayProperties {

    private Routes routes = new Routes();
    private RateLimit rateLimit = new RateLimit();
    private Cors cors = new Cors();

    @Getter
    @Setter
    public static class Routes {
        private ServiceUri accountService = new ServiceUri();
        private ServiceUri transactionService = new ServiceUri();
        private ServiceUri notificationService = new ServiceUri();
        private ServiceUri advisorService = new ServiceUri();
    }

    @Getter
    @Setter
    public static class ServiceUri {
        private String uri;
    }

    @Getter
    @Setter
    public static class RateLimit {
        private Bucket publicBucket = new Bucket(5, 10);
        private Bucket user = new Bucket(20, 40);

        @Getter
        @Setter
        public static class Bucket {
            private int replenishRate;
            private int burstCapacity;

            public Bucket() {
            }

            public Bucket(int replenishRate, int burstCapacity) {
                this.replenishRate = replenishRate;
                this.burstCapacity = burstCapacity;
            }
        }
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
    }
}

package com.cenlottery.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StartupBanner implements ApplicationRunner {

    private final Environment env;
    private final AdminKeyHolder adminKeyHolder;

    public StartupBanner(Environment env, AdminKeyHolder adminKeyHolder) {
        this.env = env;
        this.adminKeyHolder = adminKeyHolder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String port = env.getProperty("server.port", "8080");
        System.out.println("=======================================================");
        System.out.println(" CenLottery 파워픽 backend (Spring Boot) started on http://localhost:" + port);
        System.out.println(" Admin key (for /api/admin/* test endpoints): " + adminKeyHolder.getKey());
        System.out.println("=======================================================");
    }
}

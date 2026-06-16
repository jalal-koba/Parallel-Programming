package com.example.ecosystem.nfr;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NFR #5 — Apache load balancer configuration (static infra files).
 * Runtime HA is validated manually via {@code docker compose up} and balancer-manager.
 */
@Tag("nfr5")
class Nfr5LoadBalancerConfigTests {

    private static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void httpdConfDefinesRoundRobinClusterWithTwoApps() throws Exception {
        Path httpdConf = PROJECT_ROOT.resolve("httpd.conf");
        Assumptions.assumeTrue(Files.exists(httpdConf), "httpd.conf not found at project root");

        String content = Files.readString(httpdConf);

        assertThat(content).contains("balancer://mycluster");
        assertThat(content).contains("BalancerMember \"http://app1:8081\"");
        assertThat(content).contains("BalancerMember \"http://app2:8081\"");
        assertThat(content).contains("lbmethod=byrequests");
        assertThat(content).contains("/balancer-manager");
        assertThat(content).contains("ProxyPass /balancer-manager !");
    }

    @Test
    void dockerComposeDefinesLoadBalancedStack() throws Exception {
        Path compose = PROJECT_ROOT.resolve("docker-compose.yml");
        Assumptions.assumeTrue(Files.exists(compose), "docker-compose.yml not found");

        String content = Files.readString(compose);

        assertThat(content).contains("app1:");
        assertThat(content).contains("app2:");
        assertThat(content).contains("apache-lb:");
        assertThat(content).contains("SERVER_PORT=8081");
        assertThat(content).contains("9999:80");
    }

    @Test
    void devComposeExposesSingleAppOnPort8080() throws Exception {
        Path devCompose = PROJECT_ROOT.resolve("docker-compose.dev.yml");
        Assumptions.assumeTrue(Files.exists(devCompose), "docker-compose.dev.yml not found");

        String content = Files.readString(devCompose);

        assertThat(content).contains("8080:8080");
        assertThat(content).doesNotContain("apache-lb");
    }

    @Test
    void staticLandingPageDocumentsBalancer() throws Exception {
        Path indexHtml = PROJECT_ROOT.resolve("src/main/resources/static/index.html");
        Assumptions.assumeTrue(Files.exists(indexHtml), "index.html not found");

        String content = Files.readString(indexHtml);

        assertThat(content).contains("/balancer-manager");
        assertThat(content).contains("NFR #5");
    }
}

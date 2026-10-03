package kr.co.jobhub;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RobotsPolicyTest {
    @Test
    void selectsSpecificAgentRulesBeforeWildcardRules() {
        String robots = """
                User-agent: *
                Disallow: /search
                Allow: /search/public

                User-agent: PublicJobHub
                Disallow: /private
                """;

        var rules = RobotsPolicy.parse(robots, "PublicJobHub");

        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).path()).isEqualTo("/private");
    }

    @Test
    void fallsBackToWildcardGroup() {
        String robots = """
                User-agent: *
                Disallow: /search
                Allow: /search/public
                """;

        assertThat(RobotsPolicy.parse(robots, "PublicJobHub")).hasSize(2);
    }
}

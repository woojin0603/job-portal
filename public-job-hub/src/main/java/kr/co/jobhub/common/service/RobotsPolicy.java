package kr.co.jobhub.common.service;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** robots.txt를 확인해 명시적으로 금지된 URL을 수집 대상에서 제외한다. */
@Component
public class RobotsPolicy {
    private static final String USER_AGENT = "PublicJobHub";
    private static final Duration CACHE_TTL = Duration.ofHours(12);

    record Rule(boolean allow, String path) {
    }

    private record Cached(Instant loadedAt, List<Rule> rules) {
    }

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    /** 대상 호스트의 robots.txt에서 PublicJobHub 또는 일반 크롤러 규칙을 적용한다. */
    public boolean allows(URI target) {
        if (!"https".equalsIgnoreCase(target.getScheme()) || target.getHost() == null) {
            return false;
        }
        String origin = "https://" + target.getHost()
                + (target.getPort() == -1 ? "" : ":" + target.getPort());
        Cached cached = cache.get(origin);
        if (cached == null || cached.loadedAt().plus(CACHE_TTL).isBefore(Instant.now())) {
            cached = new Cached(Instant.now(), load(URI.create(origin + "/robots.txt")));
            cache.put(origin, cached);
        }
        String path = target.getRawPath() == null || target.getRawPath().isBlank() ? "/" : target.getRawPath();
        if (target.getRawQuery() != null) {
            path += "?" + target.getRawQuery();
        }
        Rule winner = null;
        for (Rule rule : cached.rules()) {
            if (matches(rule.path(), path)
                    && (winner == null || rule.path().length() > winner.path().length()
                    || (rule.path().length() == winner.path().length() && rule.allow()))) {
                winner = rule;
            }
        }
        return winner == null || winner.allow();
    }

    /** robots.txt를 가져오지 못하면 우회하지 않고 안전하게 수집을 중단한다. */
    private List<Rule> load(URI robotsUri) {
        try {
            HttpRequest request = HttpRequest.newBuilder(robotsUri)
                    .header("User-Agent", USER_AGENT + "/0.3")
                    .timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                return List.of();
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of(new Rule(false, "/"));
            }
            return parse(response.body(), USER_AGENT);
        } catch (Exception e) {
            return List.of(new Rule(false, "/"));
        }
    }

    /** 가장 구체적인 사용자 에이전트 그룹을 골라 Allow/Disallow 규칙을 읽는다. */
    static List<Rule> parse(String text, String userAgent) {
        List<Group> groups = new ArrayList<>();
        Group current = null;
        boolean hasRules = false;
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.replaceFirst("#.*$", "").trim();
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String key = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(colon + 1).trim();
            if (key.equals("user-agent")) {
                if (current == null || hasRules) {
                    current = new Group();
                    groups.add(current);
                    hasRules = false;
                }
                current.agents.add(value.toLowerCase(Locale.ROOT));
            } else if ((key.equals("allow") || key.equals("disallow")) && current != null) {
                hasRules = true;
                if (!value.isBlank()) {
                    current.rules.add(new Rule(key.equals("allow"), value));
                }
            }
        }
        String requested = userAgent.toLowerCase(Locale.ROOT);
        List<Rule> exact = groups.stream()
                .filter(g -> g.agents.stream().anyMatch(requested::startsWith))
                .flatMap(g -> g.rules.stream()).toList();
        if (!exact.isEmpty()) {
            return exact;
        }
        return groups.stream().filter(g -> g.agents.contains("*"))
                .flatMap(g -> g.rules.stream()).toList();
    }

    private static boolean matches(String rule, String path) {
        boolean end = rule.endsWith("$");
        String value = end ? rule.substring(0, rule.length() - 1) : rule;
        String regex = Pattern.quote(value).replace("*", "\\E.*\\Q");
        return Pattern.compile("^" + regex + (end ? "$" : ".*")).matcher(path).matches();
    }

    private static final class Group {
        private final List<String> agents = new ArrayList<>();
        private final List<Rule> rules = new ArrayList<>();
    }
}

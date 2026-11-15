package kr.co.jobhub.common.service;

import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;

/** 서버가 외부 URL을 읽기 전에 내부망·로컬 주소로 향하는 요청을 차단한다. */
@Service
public class SafeExternalUrl {
    public URI requirePublicHttps(String raw) {
        URI uri = URI.create(raw == null ? "" : raw.trim());
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
            throw new IllegalArgumentException("공개 HTTPS 주소만 사용할 수 있습니다.");
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".local"))
            throw new IllegalArgumentException("로컬 주소는 사용할 수 없습니다.");
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress() || address.isMulticastAddress())
                    throw new IllegalArgumentException("내부 네트워크 주소는 사용할 수 없습니다.");
            }
        } catch (java.net.UnknownHostException exception) {
            throw new IllegalArgumentException("주소를 확인할 수 없습니다.", exception);
        }
        return uri;
    }
}

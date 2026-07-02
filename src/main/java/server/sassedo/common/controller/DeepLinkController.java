package server.sassedo.common.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;

@RestController
@RequestMapping("/api/deeplink")
@RequiredArgsConstructor
public class DeepLinkController {

    @GetMapping("/**")
    public ResponseEntity<Void> fallback(HttpServletRequest request) {
        String fullPath = request.getRequestURI();
        String path = fullPath.replaceFirst("/api/deeplink", "");
        if (path.isEmpty()) path = "/";

        String userAgent = request.getHeader("User-Agent");
        String redirectTo = getString(userAgent);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectTo))
                .build();
    }

    private static String getString(String userAgent) {
        boolean isAndroid = userAgent != null && userAgent.toLowerCase().contains("android");
        boolean isIos = userAgent != null && (userAgent.toLowerCase().contains("iphone")
                || userAgent.toLowerCase().contains("ipad"));

        String redirectTo = "https://nineteen.bg";

        if (isAndroid) {
            redirectTo = "https://nineteen.bg/googleplay";
        } else if (isIos) {
            redirectTo = "https://nineteen.bg/appstore";
        }
        return redirectTo;
    }
}

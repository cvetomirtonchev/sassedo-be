package server.sassedo.model;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import server.sassedo.security.jwt.JwtUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class LoggingFilter implements Filter {
    private final JwtUtils jwtUtils;

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    /**
     * Max bytes of request body to buffer for logging (Spring Framework 7+ requires an explicit limit).
     */
    private static final int REQUEST_CONTENT_CACHE_LIMIT = 16 * 1024;

    /**
     * When {@code false} (the production default) the filter does not log successful requests and
     * never buffers request bodies on the hot path; it only records a concise line for error
     * responses (HTTP status >= 400). Buffering + masking + multi-line logging on every request is
     * expensive under traffic floods, so keep verbose logging off outside of debugging.
     */
    @Value("${sassedo.logging.requests.verbose:false}")
    private boolean verboseRequestLogging;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if (verboseRequestLogging) {
            doFilterVerbose(req, res, chain);
            return;
        }

        // Production default: wrap only the response (cheap) so we can surface error bodies,
        // but skip request-body buffering/masking and per-request success logging entirely.
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(res);
        try {
            chain.doFilter(req, wrappedResponse);
        } finally {
            int status = wrappedResponse.getStatus();
            if (status >= 400) {
                String uri = req.getRequestURI();
                String queryString = req.getQueryString();
                if (queryString != null) {
                    uri += "?" + queryString;
                }
                String responseBody = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
                logger.info("{} {} -> {} {}", req.getMethod(), uri, status, responseBody);
            }
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void doFilterVerbose(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(req, REQUEST_CONTENT_CACHE_LIMIT);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(res);

        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            String requestBody = getRequestBody(wrappedRequest);
            logRequest(wrappedRequest, requestBody);
            logResponse(wrappedResponse);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] buf = request.getContentAsByteArray();
        String contentString = new String(buf, StandardCharsets.UTF_8);
        return maskSensitiveData(contentString);
    }

    private String maskSensitiveData(String data) {
        data = data.replaceAll("(\"password\"\\s*:\\s*\")[^\"]*\"", "$1****\"");
        data = data.replaceAll("(\"token\"\\s*:\\s*\")[^\"]*\"", "$1****\"");
        data = data.replaceAll("(\"newPassword\"\\s*:\\s*\")[^\"]*\"", "$1****\"");
        data = data.replaceAll("(\"oldPassword\"\\s*:\\s*\")[^\"]*\"", "$1****\"");
        return data;
    }

    private void logRequest(HttpServletRequest request, String body) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        if (queryString != null) {
            uri += "?" + queryString;
        }
        logger.info("Request {} {}", method, uri);

        String token = request.getHeader("Authorization");
        if (token != null) {
            try {
                String bearerToken = token.startsWith("Bearer ") ? token.substring(7) : token;
                Long userId = jwtUtils.extractUserId(bearerToken);
                String username = jwtUtils.getUserNameFromJwtToken(bearerToken);
                logger.info("User ID: {}, username: {}", userId, username);
            } catch (Exception e) {
                logger.debug("Could not extract user info from token");
            }
        }
        if (method.equals("POST") || method.equals("PUT") || method.equals("PATCH") || method.equals("DELETE")) {
            logger.info("Request body: {}", body);
        }
    }

    private void logResponse(ContentCachingResponseWrapper response) {
        int status = response.getStatus();
        logger.info("Response status: {}", status);
        if (status >= 400) {
            String responseBody = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
            logger.info("Error response body: {}", responseBody);
        }
    }
}

package com.smartbasket.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Gate the server with a shared API key when one is configured (set
 * {@code SMARTBASKET_API_KEY} in prod). If unset (local dev), all requests pass.
 *
 * <p>ponytail: a single shared key is enough to stop open access on a hosted box.
 * Per-user identity (drop the userId tool param, derive from the caller) is the next
 * step — tracked in issue #1. Upgrade to Cognito/JWT if you need managed accounts.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private final String apiKey;

    public ApiKeyFilter(@Value("${smartbasket.api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (StringUtils.hasText(apiKey) && !apiKey.equals(request.getHeader("X-API-Key"))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing X-API-Key");
            return;
        }
        chain.doFilter(request, response);
    }
}

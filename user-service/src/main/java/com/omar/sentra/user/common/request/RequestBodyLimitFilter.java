package com.omar.sentra.user.common.request;

import com.omar.sentra.user.common.error.ApiErrorFactory;
import com.omar.sentra.user.common.error.ServiceErrors;
import com.omar.sentra.user.common.error.UserServiceException;
import com.omar.sentra.user.config.UserServiceProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Enforces the documented mutation body limit for both fixed-length and chunked
 * requests.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestBodyLimitFilter extends OncePerRequestFilter {
    private final int maximumBytes;
    private final ApiErrorFactory errorFactory;
    private final JsonMapper jsonMapper;

    public RequestBodyLimitFilter(
            UserServiceProperties properties,
            ApiErrorFactory errorFactory,
            JsonMapper jsonMapper) {
        this.maximumBytes = properties.limits().maxRequestBodyBytes();
        this.errorFactory = errorFactory;
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"PATCH".equals(request.getMethod())
                || !"/internal/v1/users/me".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        byte[] body = request.getInputStream().readNBytes(maximumBytes + 1);
        if (body.length > maximumBytes) {
            writeError(request, response, ServiceErrors.bodyTooLarge());
            return;
        }
        filterChain.doFilter(new CachedBodyRequest(request, body), response);
    }

    private void writeError(
            HttpServletRequest request,
            HttpServletResponse response,
            UserServiceException exception)
            throws IOException {
        response.setStatus(exception.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        jsonMapper.writeValue(response.getOutputStream(), errorFactory.create(request, exception));
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body.clone();
        }

        @Override
        public ServletInputStream getInputStream() {
            InputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    try {
                        return input.available() == 0;
                    } catch (IOException exception) {
                        return true;
                    }
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    try {
                        if (!isFinished()) {
                            readListener.onDataAvailable();
                        }
                        if (isFinished()) {
                            readListener.onAllDataRead();
                        }
                    } catch (IOException exception) {
                        readListener.onError(exception);
                    }
                }

                @Override
                public int read() throws IOException {
                    return input.read();
                }
            };
        }
    }
}

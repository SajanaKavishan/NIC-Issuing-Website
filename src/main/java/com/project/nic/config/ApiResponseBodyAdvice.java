package com.project.nic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.nic.dto.ApiDtos.ApplicationSubmissionResponse;
import com.project.nic.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;

@Component
@RestControllerAdvice(basePackages = "com.project.nic.controller")
public class ApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {
    private final ObjectMapper objectMapper;

    public ApiResponseBodyAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (!isApiRequest(request) || shouldSkip(body, selectedContentType)) {
            return body;
        }

        ApiResponse<?> wrapped = wrap(body, request, response);
        if (body instanceof String) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            try {
                return objectMapper.writeValueAsString(wrapped);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to write API response", e);
            }
        }

        return wrapped;
    }

    private ApiResponse<?> wrap(Object body, ServerHttpRequest request, ServerHttpResponse response) {
        String path = path(request);
        boolean success = statusCode(response).is2xxSuccessful();

        if (success) {
            return body instanceof String message
                    ? ApiResponse.success(null, message, path)
                    : ApiResponse.success(body, path);
        }

        String message = body instanceof String text ? text : "Request failed";
        return ApiResponse.error(message, List.of(), path);
    }

    private boolean isApiRequest(ServerHttpRequest request) {
        return path(request).startsWith("/api/");
    }

    private String path(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
            return httpServletRequest.getRequestURI();
        }
        return request.getURI().getPath();
    }

    private HttpStatusCode statusCode(ServerHttpResponse response) {
        if (response instanceof ServletServerHttpResponse servletResponse) {
            return HttpStatusCode.valueOf(servletResponse.getServletResponse().getStatus());
        }
        return HttpStatusCode.valueOf(200);
    }

    private boolean shouldSkip(Object body, MediaType selectedContentType) {
        if (body instanceof ApiResponse<?> || body instanceof ApplicationSubmissionResponse || body instanceof Resource || body instanceof byte[]) {
            return true;
        }
        return selectedContentType != null
                && (MediaType.APPLICATION_OCTET_STREAM.includes(selectedContentType)
                || MediaType.IMAGE_JPEG.includes(selectedContentType)
                || MediaType.IMAGE_PNG.includes(selectedContentType)
                || MediaType.APPLICATION_PDF.includes(selectedContentType));
    }
}

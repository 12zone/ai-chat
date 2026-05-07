package org.example.qasystem.config;
//统一处理成功响应

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.example.qasystem.model.ApiResponse;

@RestControllerAdvice(basePackages = "org.example.qasystem.controller")
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public ResponseAdvice(ObjectMapper objectMapper){this.objectMapper = objectMapper;}

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType){
        return !returnType.getParameterType().equals(ApiResponse.class);
    }

    @SneakyThrows
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response){
        if(body instanceof ApiResponse){
            return body;
        }

        if(body instanceof String){
            return objectMapper.writeValueAsString(ApiResponse.success(body));
        }

        return ApiResponse.success(body);
    }
}

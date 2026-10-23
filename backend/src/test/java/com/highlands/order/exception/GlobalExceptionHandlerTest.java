package com.highlands.order.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNoResourceFound_returnsNotFound() {
        MockHttpServletRequest request = request("GET", "/api/v1/missing");
        var response = handler.handleNoResourceFound(
                new NoResourceFoundException(HttpMethod.GET, "/api/v1/missing", ""),
                request
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(ApiErrorCode.ROUTE_NOT_FOUND.name(), response.getBody().errorCode());
        assertEquals("/api/v1/missing", response.getBody().path());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    void handleAuthenticationException_returnsUnauthorized() {
        var response = handler.handleAuthenticationException(
                new BadCredentialsException("Email hoặc mật khẩu không đúng"),
                request("POST", "/api/v1/auth/login")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(ApiErrorCode.AUTHENTICATION_FAILED.name(), response.getBody().errorCode());
    }

    @Test
    void handleDomainException_preservesCodeAndStatus() {
        var response = handler.handleDomainException(
                new ApiDomainException(
                        ApiErrorCode.COUPON_EXPIRED,
                        HttpStatus.BAD_REQUEST,
                        "Mã giảm giá đã hết hạn"
                ),
                request("POST", "/api/v1/coupons/validate")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ApiErrorCode.COUPON_EXPIRED.name(), response.getBody().errorCode());
        assertEquals("/api/v1/coupons/validate", response.getBody().path());
    }

    @Test
    void handleGeneralException_hidesInternalMessage() {
        var response = handler.handleGeneralException(
                new RuntimeException("internal-database-detail"),
                request("GET", "/api/v1/test")
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(ApiErrorCode.INTERNAL_ERROR.name(), response.getBody().errorCode());
        assertEquals("Đã xảy ra lỗi hệ thống", response.getBody().message());
    }

    @Test
    void handleMalformedJson_returnsStableErrorCode() {
        var response = handler.handleMalformedJson(
                mock(HttpMessageNotReadableException.class),
                request("POST", "/api/v1/orders")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ApiErrorCode.MALFORMED_JSON.name(), response.getBody().errorCode());
    }

    @Test
    void handleTypeMismatch_returnsParameterName() {
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn("id");

        var response = handler.handleTypeMismatch(
                exception,
                request("GET", "/api/v1/products/not-a-number")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ApiErrorCode.INVALID_PARAMETER.name(), response.getBody().errorCode());
        assertEquals("Tham số 'id' không đúng định dạng", response.getBody().message());
    }

    private MockHttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }
}

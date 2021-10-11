package skyglass.composer.product.exception;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.NestedServletException;

@ControllerAdvice
public class CustomRestExceptionHandler extends ResponseEntityExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(CustomRestExceptionHandler.class);

	@Autowired
	private UserContext userContext;

	@Autowired
	private Jaxb2RootElementHttpMessageConverter jaxb2RootElementHttpMessageConverter;

	@NotNull
	@ResponseBody
	@ExceptionHandler({ Exception.class })
	public ResponseEntity<?> handleExceptions(Exception ex, WebRequest request) {
		return handleExceptions(ex, HttpClientUtil.toHttpServletRequest(request));
	}

	@NotNull
	public ResponseEntity<?> handleExceptions(Throwable ex, HttpServletRequest request) {
		HttpHeaders headers = new HttpHeaders();

		if (ex instanceof SkyglassException) {
			SkyglassException e = (SkyglassException) ex;
			return handleExceptionInternal(e, e.getStatusText(), e.getResponseBody(), e.getPayload(), headers,
					HttpStatus.valueOf(e.getRawStatusCode()), request);
		} else if (ex instanceof RestClientResponseException) {
			RestClientResponseException e = (RestClientResponseException) ex;
			return handleExceptionInternal(e, e.getStatusText(), e.getResponseBodyAsString(), null, headers,
					HttpStatus.valueOf(e.getRawStatusCode()), request);
		}

		return handleExceptionInternal(ex, headers, request);
	}

	@NotNull
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {
		return handleExceptionInternal(ex, body, headers, status, HttpClientUtil.toHttpServletRequest(request));
	}

	@NotNull
	protected ResponseEntity<Object> handleExceptionInternal(Throwable ex, HttpHeaders headers, HttpServletRequest request) {
		return handleExceptionInternal(ex, ex != null ? ex.getMessage() : null, headers, null, request);
	}

	@NotNull
	protected ResponseEntity<Object> handleExceptionInternal(Throwable ex, Object body, HttpHeaders headers, HttpStatus status, HttpServletRequest request) {
		if (status == null) {
			status = getStatus(request);
		}

		return handleExceptionInternal(ex, status.name(), body, null, headers, status, request);
	}

	@NotNull
	protected ResponseEntity<Object> handleExceptionInternal(Throwable ex, String message, Object body, Serializable payload, HttpHeaders headers, HttpStatus status, HttpServletRequest request) {
		if (request == null) {
			try {
				request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
			} catch (Exception e) {
				log.warn("Could not fetch servlet request from RequestContextHolder", e);
			}
		}

		if (status == null) {
			status = getStatus(request);
		}

		String msg = status.value() + " " + message;

		String username = null;
		if (request != null) {
			Principal principal = request.getUserPrincipal();
			if (principal != null && principal instanceof Authentication) {
				username = userContext.getUsernameFromAuthentication((Authentication) principal);
				if (username != null) {
					username = username.toUpperCase();
				}
			}
		}

		if (!StringUtils.isBlank(username)) {
			msg = "[" + username + "] " + msg;
		}

		if (body != null) {
			msg += ": " + body;
		}

		logCompleteStack(msg, ex);

		if (body == null && ex != null) {
			body = StringUtils.trimToEmpty(ex.getMessage());
		}

		String path = null;
		String method = null;

		if (request != null) {
			path = new ServletServerHttpRequest(request).getURI().toString();
			method = request.getMethod();
		}

		ApiError apiError = ErrorUtil.createApiError(status, message, body != null ? body.toString() : null, path, method, payload, MDC.get("correlation_id"), ex);
		apiError.setJsonRequest(getJsonPayload(request));

		// ClientAbortExceptions are e.g. broken pipe or connection reset by peer
		// NestedServletExceptions are e.g. handler dispatch failed in outputstream
		if (!(ex instanceof ClientAbortException) && !(ex instanceof NestedServletException)) {
			ErrorUtil.sendErrorToSlack(username, apiError, ex);
		}

		return response(apiError, headers, request);
	}

	@NotNull
	private ResponseEntity<Object> response(ApiError apiError, HttpHeaders httpHeaders, HttpServletRequest request) {
		if (httpHeaders == null) {
			httpHeaders = new HttpHeaders();
		}

		if (HttpStatus.INTERNAL_SERVER_ERROR.value() == apiError.getStatus()) {
			if (CloudUtil.isProductionSpace()) {
				apiError.setBody(null);
				apiError.setPath(null);
			}
		}

		String accept = null;
		if (request != null) {
			accept = request.getHeader(HttpHeaders.ACCEPT);
		}
		MediaType acceptMediaType = null;
		try {
			acceptMediaType = MediaType.valueOf(accept);
		} catch (Exception e) {
			log.error("Given MediaType is not valid: " + accept, e);
		}
		if (acceptMediaType != null && jaxb2RootElementHttpMessageConverter.getSupportedMediaTypes().contains(acceptMediaType)) {
			httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_XML));
			httpHeaders.setContentType(MediaType.APPLICATION_XML);
		} else {
			httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));
			httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
		}

		return new ResponseEntity<>(apiError, httpHeaders, HttpStatus.valueOf(apiError.getStatus()));
	}

	@NotNull
	private HttpStatus getStatus(HttpServletRequest request) {
		Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
		if (statusCode == null) {
			return HttpStatus.INTERNAL_SERVER_ERROR;
		}

		return HttpStatus.valueOf(statusCode);
	}

	private String getJsonPayload(HttpServletRequest request) {
		if (request != null && request instanceof RequestCachingRequestWrapper) {
			RequestCachingRequestWrapper requestWrapper = (RequestCachingRequestWrapper) request;
			return requestWrapper.getCache().toString();
		}

		return null;
	}

	private void logCompleteStack(String message, Throwable cause) {
		if (cause == null) {
			return;
		}

		log.error(message, cause);

		Throwable causedBy = cause.getCause();
		if (causedBy != null) {
			logCompleteStack("Caused by", causedBy);
		}
	}
}

package skyglass.composer.product.exception;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

public class SkyglassException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final int rawStatusCode;

	private final String statusText;

	private final Object responseBody;

	private final Serializable payload;

	public SkyglassException(Throwable cause) throws IllegalArgumentException {
		this(HttpStatus.INTERNAL_SERVER_ERROR, cause);
	}

	public SkyglassException(int statusCode, Throwable cause) throws IllegalArgumentException {
		this(statusCode, null, cause);
	}

	public SkyglassException(int statusCode, Object body) throws IllegalArgumentException {
		this(statusCode, body, null);
	}

	public SkyglassException(int statusCode, Object body, Throwable cause) throws IllegalArgumentException {
		this(HttpStatus.valueOf(statusCode), body, cause);
	}

	public SkyglassException(HttpStatus status, Throwable cause) throws IllegalArgumentException {
		this(status, null, cause);
	}

	public SkyglassException(Object body) throws IllegalArgumentException {
		this(body, null);
	}

	public SkyglassException(Object body, Throwable cause) throws IllegalArgumentException {
		this(HttpStatus.INTERNAL_SERVER_ERROR, body, cause);
	}

	public SkyglassException(HttpStatus status, Object body) throws IllegalArgumentException {
		this(status, body, null);
	}

	public SkyglassException(HttpStatus status, Object body, Throwable cause) throws IllegalArgumentException {
		this(status, body == null || StringUtils.isBlank(body.toString()) ? (cause != null ? cause.getMessage() : null) : body, null, cause);
	}

	public SkyglassException(HttpStatus status, Object body, Serializable payload, Throwable cause) throws IllegalArgumentException {
		super(buildMessage(status, body), cause);

		this.rawStatusCode = status.value();
		this.statusText = status.name();
		this.responseBody = body != null ? body : "";
		this.payload = payload;

		if (!status.is4xxClientError() && !status.is5xxServerError()) {
			throw new IllegalArgumentException("Status code cannot be below 400: " + status);
		}
	}

	private static String buildMessage(HttpStatus status, Object body) {
		if (status == null) {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}

		String msg = status.value() + " " + status.name();
		if (body != null) {
			msg += " " + body;
		}

		return msg;
	}

	public int getRawStatusCode() {
		return this.rawStatusCode;
	}

	public String getStatusText() {
		return this.statusText;
	}

	public Object getResponseBody() {
		return this.responseBody;
	}

	public Serializable getPayload() {
		return this.payload;
	}
}

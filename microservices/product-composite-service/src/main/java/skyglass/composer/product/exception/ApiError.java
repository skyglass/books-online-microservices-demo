package skyglass.composer.product.exception;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.http.HttpStatus;

@XmlRootElement
public class ApiError implements Serializable {
	private static final long serialVersionUID = 23587645312L;

	private int status = HttpStatus.INTERNAL_SERVER_ERROR.value();

	private long timestamp;

	private String message = null;

	private String body = null;

	private String path = null;

	private String method = null;

	private String correlationId = null;

	private String exception;

	private String jsonRequest = null;

	private Serializable payload = null;

	public ApiError() {
		this(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	public ApiError(HttpStatus status) {
		this(status == null ? HttpStatus.INTERNAL_SERVER_ERROR.value() : status.value(),
				status == null ? HttpStatus.INTERNAL_SERVER_ERROR.name() : status.name(), null);
	}

	public ApiError(int status, String message, String body) throws IllegalArgumentException {
		this(status, message, body, null);
	}

	public ApiError(int status, String message, String body, Class<? extends Throwable> exception)
			throws IllegalArgumentException {
		this(status, message, body, null, exception);
	}

	public ApiError(int status, String message, String body, String path, Class<? extends Throwable> exception)
			throws IllegalArgumentException {
		this(status, message, body, path, null, exception);
	}

	public ApiError(int status, String message, String body, String path, String method, Class<? extends Throwable> exception) throws IllegalArgumentException {
		this(status, message, body, path, method, null, exception);
	}

	public ApiError(int status, String message, String body, String path, String method, Serializable payload, Class<? extends Throwable> exception) throws IllegalArgumentException {
		this(status, message, body, path, method, payload, null, exception);
	}

	public ApiError(int status, String message, String body, String path, String method, Serializable payload, String correlationId, Class<? extends Throwable> exception)
			throws IllegalArgumentException {
		if (status < 400 || status > 599) {
			throw new IllegalStateException("Status needs to be between 400 and 599, but was " + status);
		}

		this.status = status;
		this.body = body;
		this.message = message;
		this.path = path;
		this.method = method;
		this.payload = payload;
		this.correlationId = correlationId;
		this.exception = exception != null ? exception.getName() : null;
		this.timestamp = System.currentTimeMillis();
	}

	public long getTimestamp() {
		return this.timestamp;
	}

	public int getStatus() {
		return this.status;
	}

	public String getMessage() {
		return this.message;
	}

	public String getBody() {
		return this.body;
	}

	public String getPath() {
		return this.path;
	}

	public String getMethod() {
		return this.method;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public String getException() {
		return this.exception;
	}

	public String getJsonRequest() {
		return this.jsonRequest;
	}

	@XmlTransient
	public Serializable getPayload() {
		return this.payload;
	}

	public void setStatus(int status) throws IllegalStateException {
		if (status < 400 || status > 599) {
			throw new IllegalStateException("Status needs to be between 400 and 599, but was " + status);
		}

		this.status = status;
	}

	public void setMessage(String message) {

		this.message = message;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public void setException(String exception) {
		this.exception = exception;
	}

	public void setJsonRequest(String jsonRequest) {
		this.jsonRequest = jsonRequest;
	}

	public void setPayload(Serializable payload) {
		this.payload = payload;
	}

	@Override
	public String toString() {
		ToStringBuilder builder = new ToStringBuilder(this);
		builder.append(this.timestamp);
		builder.append(this.status);
		builder.append(this.message);
		builder.append(this.path);
		builder.append(this.method);
		builder.append(this.exception);
		builder.append(this.body);
		builder.append(this.jsonRequest);
		builder.append(this.payload);

		return builder.build();
	}
}

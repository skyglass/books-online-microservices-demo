package skyglass.composer.product.exception;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class ErrorUtil {
	private static final Logger log = LoggerFactory.getLogger(ErrorUtil.class);

	private static final String SPACE_NAME = CloudUtil.getSpaceName();

	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

	public static ApiError createApiError(HttpStatus status, String message, String body, String path, String method, Serializable payload, Throwable ex) {
		return new ApiError(status.value(), message == null ? status.name() : message, body, path, method, payload, ex.getClass());
	}

	public static ApiError createApiError(HttpStatus status, String message, String body, String path, String method, Serializable payload, String correlationId, Throwable ex) {
		return new ApiError(status.value(), message == null ? status.name() : message, body, path, method, payload, correlationId, ex.getClass());
	}

	public static void sendErrorToSlack(String username, ApiError apiError, Throwable cause) {
		int httpStatus = apiError.getStatus();
		if (httpStatus < 500) {
			// Do not send errors below 500 status anymore
			return;
		}

		String url;

		switch (SPACE_NAME) {
			case CloudUtil.SPACE_NAME_DEVELOPMENT:
				url = "https://hooks.slack.com/services/";
				return;
			case CloudUtil.SPACE_NAME_INTEGRATION:
				url = "no-channel-yet";
				return;
			case CloudUtil.SPACE_NAME_QA:
				url = "https://hooks.slack.com/services/";
				return;
			case CloudUtil.SPACE_NAME_PRODUCTION:
				url = "https://hooks.slack.com/services/";
				break;
			default:
				log.error("Determined space name not supported for logging to Slack: '" + SPACE_NAME + "'");

				return;
		}

		String user = "";
		if (!StringUtils.isBlank(username)) {
			user = " for user *" + username + "*";
		}

		String stackTrace = ExceptionUtils.getStackTrace(cause).replaceAll("\"", "'");
		if (stackTrace.length() > 2000) {
			stackTrace = StringUtils.abbreviateMiddle(stackTrace, "...\n...", 2000);
		}

		String text = "An error occured" + user + " at " + dateFormat.format(new Date(apiError.getTimestamp())) + " ("
				+ apiError.getTimestamp() + "):\n*" + httpStatus + " " + apiError.getMessage() + "*";
		if (!StringUtils.isBlank(Objects.toString(apiError.getBody()))) {
			text += ": " + apiError.getBody();
		}
		text += "\n";
		if (!StringUtils.isBlank(apiError.getPath())) {
			text += "Path: " + apiError.getPath();
			if (!StringUtils.isBlank(apiError.getMethod())) {
				text += " (" + apiError.getMethod() + ")";
			}
			text += "\n";
		}
		if (!StringUtils.isBlank(apiError.getCorrelationId())) {
			text += "Correlation ID: " + apiError.getCorrelationId();
			text += "\n";
		}
		if (!StringUtils.isBlank(apiError.getJsonRequest())) {
			text += "JSON Request:\n```\n" + apiError.getJsonRequest().replace("\"", "\\\"") + "```";
			text += "\n";
		}
		text += "Stacktrace for *" + apiError.getException() + "*:\n```\n" + stackTrace + "```";

		SlackUtil.sendMessageAsync(url, "#logs-" + SPACE_NAME, "skyglass", ":skyglass:", text);
	}
}

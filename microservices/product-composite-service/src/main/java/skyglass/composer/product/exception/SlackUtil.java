package skyglass.composer.product.exception;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class SlackUtil {
	private static final Logger log = LoggerFactory.getLogger(SlackUtil.class);

	private static final ScheduledExecutorService scheduledExecutor = new ScheduledThreadPoolExecutor(1);

	public static void sendMessageAsync(String url, String channel, String name, String icon, String message) {
		scheduledExecutor.schedule(() -> {
			try {
				sendMessage(url, channel, name, icon, message);
			} catch (Exception ex) {
				log.error("Failed to send Slack messsage", ex);
			}
		}, 0L, TimeUnit.MILLISECONDS);
	}

	public static void sendMessage(String url, String channel, String name, String icon, String message) throws Exception {
		if (StringUtils.isBlank(channel)) {
			throw new IllegalArgumentException("channel cannot be empty");
		}

		if (!channel.startsWith("#")) {
			channel = "#" + channel;
		}

		if (StringUtils.isBlank(name)) {
			name = "skyglass";
		}

		if (StringUtils.isBlank(icon)) {
			name = ":skyglass:";
		} else {
			if (!icon.startsWith(":")) {
				icon = ":" + icon;
			}

			if (!icon.endsWith(":")) {
				icon += ":";
			}
		}

		String payload = "{\"channel\": \"" + channel + "\", \"username\": \"" + name + "\", \"text\": \"" + message
				+ "\", \"icon_emoji\": \"" + icon + "\", \"mrkdwn\": true}";

		log.debug("Sending Slack message '" + payload + "' to '" + url + "' (" + channel + ")");

		try {
			String response = send(url, createRequest(payload, MediaType.APPLICATION_JSON_UTF8), String.class);
			if (!"ok".equals(response)) {
				throw new RestClientException("Unexpected response: " + response);
			}
		} catch (RestClientException ex) {
			throw new IOException("Failed to post Slack message '" + payload + "' to '" + url + "'", ex);
		}
	}

	private static <V extends Object> V send(String url, HttpEntity<?> request, Class<? extends V> returnType)
			throws RestClientException {
		RestTemplate template = new RestTemplate();

		return template.postForObject(url, request, returnType);
	}

	private static <V extends Object> HttpEntity<V> createRequest(V payload, MediaType contentType) {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(contentType);

		return new HttpEntity<>(payload, httpHeaders);
	}
}

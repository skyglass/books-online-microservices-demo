package skyglass.composer.product.exception;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpClientUtil {
	private static final Logger log = LoggerFactory.getLogger(HttpClientUtil.class);

	public static final String HEADER_AUTHORIZATION = "Authorization";

	public static final String HEADER_BEARER_PREFIX = "Bearer ";

	public static final String HEADER_BASIC_PREFIX = "Basic ";

	public static final String PROTOCOL_HTTP_PREFIX = "http://";

	public static final String PARAM_HTTP_CLIENT = "HttpClient";

	private static final ObjectMapper mapper = new ObjectMapper();

	static {
		mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
		mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
		mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	}

	public static void addBasicAuth(HttpUriRequest request, String user, String password) {
		addBasicAuth(request, base64encode(user, password));
	}

	public static void addBasicAuth(HttpUriRequest request, String usernamePasswordBase64) {
		setHeader(request, HEADER_AUTHORIZATION, HEADER_BASIC_PREFIX + usernamePasswordBase64);
	}

	public static void addDefaultHeaders(HttpUriRequest request) {
		request.setHeaders(getDefaultHeaders());
	}

	public static void addHeader(HttpUriRequest request, String key, String... values) {
		if (values != null) {
			for (String value : values) {
				request.addHeader(key, value);
			}
		}
	}

	public static void addHeaders(HttpUriRequest request, Map<String, String> headers) {
		for (String key : headers.keySet()) {
			request.addHeader(key, headers.get(key));
		}
	}

	public static void setHeader(HttpUriRequest request, String key, String... values) {
		request.removeHeaders(key);

		addHeader(request, key, values);
	}

	public static void setHeaders(HttpUriRequest request, Map<String, String> headers) {
		for (String key : headers.keySet()) {
			request.removeHeaders(key);

			request.addHeader(key, headers.get(key));
		}
	}

	public static void addToken(HttpUriRequest request, String token) {
		setHeader(request, HEADER_AUTHORIZATION, HEADER_BEARER_PREFIX + token);
	}

	public static Header[] getDefaultHeaders() {
		return new Header[] { new BasicHeader("Accept", "application/json"),
				new BasicHeader("Content-type", "application/json") };
	}

	@NotNull
	public static String readResponse(HttpResponse response) throws SkyglassException {
		StatusLine statusLine = response.getStatusLine();
		if (statusLine.getStatusCode() >= 300) {
			String responseReason;

			try {
				HttpEntity entity = response.getEntity();
				responseReason = EntityUtils.toString(entity, "UTF-8");
			} catch (ParseException | IOException e) {
				responseReason = "";
				log.error("Exception occurred while reading the HTTP response", e);
			}

			throw new SkyglassException(statusLine.getStatusCode(),
					statusLine.getReasonPhrase() + ": " + responseReason);
		}

		try {
			return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
		} catch (IOException | ParseException ex) {
			throw new SkyglassException(ex);
		}
	}

	@NotNull
	public static <R extends Object> R readResponse(HttpResponse response, Class<R> clazz) throws SkyglassException {
		return readResponse(response, clazz, false);
	}

	@NotNull
	private static <R extends Object> R readResponse(HttpResponse response, Class<R> clazz,
			boolean failOnUnknownProperties) throws SkyglassException {
		return readResponse(readResponse(response), clazz, failOnUnknownProperties);
	}

	@NotNull
	public static <R extends Object> R readResponse(String response, Class<R> clazz, boolean failOnUnknownProperties)
			throws SkyglassException {
		try {
			ObjectMapper objMapper = new ObjectMapper();
			objMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknownProperties);
			R result = objMapper.readValue(response, clazz);
			if (result == null) {
				throw new SkyglassException("Received response was null.");
			}

			return result;
		} catch (IOException ex) {
			throw new SkyglassException(ex);
		}
	}

	@NotNull
	public static <R extends Object> List<R> readListResponse(HttpResponse response, Class<R> clazz)
			throws SkyglassException {
		return readListResponse(response, clazz, false);
	}

	@NotNull
	public static <R extends Object> List<R> readListResponse(HttpResponse response, Class<R> clazz, boolean failOnUnknownProperties) throws SkyglassException {
		return readListResponse(readResponse(response), clazz, failOnUnknownProperties);
	}

	@NotNull
	public static <R extends Object> List<R> readListResponse(String response, Class<R> clazz)
			throws SkyglassException {
		return readListResponse(response, clazz, false);
	}

	@NotNull
	public static <R extends Object> List<R> readListResponse(String response, Class<R> clazz, boolean failOnUnknownProperties) throws SkyglassException {
		try {
			ObjectMapper objMapper = new ObjectMapper();
			objMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknownProperties);

			List<R> result = objMapper.readValue(response, objMapper.getTypeFactory().constructCollectionType(List.class, clazz));
			if (result == null) {
				throw new SkyglassException("Received response was null.");
			}

			return result;
		} catch (IOException ex) {
			throw new SkyglassException(ex);
		}
	}

	@NotNull
	public static <R extends Object> List<R> readListResponseWithInputStream(InputStream response, Class<R> clazz, boolean failOnUnknownProperties) throws SkyglassException {
		try {
			ObjectMapper objMapper = new ObjectMapper();
			objMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknownProperties);

			List<R> result = objMapper.readValue(response, objMapper.getTypeFactory().constructCollectionType(List.class, clazz));
			if (result == null) {
				throw new SkyglassException("Received response was null.");
			}

			return result;
		} catch (IOException ex) {
			throw new SkyglassException(ex);
		}
	}

	@NotNull
	public static <K extends Object, V extends Object> Map<K, V> readMapResponse(HttpResponse response, Class<K> keyClass, Class<V> valueClass) throws SkyglassException {
		return readMapResponse(response, keyClass, valueClass, false);
	}

	@NotNull
	public static <K extends Object, V extends Object> Map<K, V> readMapResponse(HttpResponse response, Class<K> keyClass, Class<V> valueClass, boolean failOnUnknownProperties)
			throws SkyglassException {
		return readMapResponse(readResponse(response), keyClass, valueClass, failOnUnknownProperties);
	}

	@NotNull
	public static <K extends Object, V extends Object> Map<K, V> readMapResponse(String response, Class<K> keyClass, Class<V> valueClass) throws SkyglassException {
		return readMapResponse(response, keyClass, valueClass, false);
	}

	@NotNull
	public static <K extends Object, V extends Object> Map<K, V> readMapResponse(String response, Class<K> keyClass, Class<V> valueClass, boolean failOnUnknownProperties) throws SkyglassException {
		try {
			ObjectMapper objMapper = new ObjectMapper();
			objMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknownProperties);

			Map<K, V> result = objMapper.readValue(response, objMapper.getTypeFactory().constructMapType(Map.class, keyClass, valueClass));
			if (result == null) {
				throw new SkyglassException("Received response was null.");
			}

			return result;
		} catch (IOException ex) {
			throw new SkyglassException(ex);
		}
	}

	public static void setPayload(HttpEntityEnclosingRequestBase request, Object payload) throws SkyglassException {
		setPayload(request, payload, null);
	}

	public static void setPayload(HttpEntityEnclosingRequestBase request, Object payload, Class transferBefore)
			throws SkyglassException {
		if (transferBefore != null) {
			try {
				payload = convertValue(payload, transferBefore);
			} catch (IllegalArgumentException ex) {
				throw new UnsupportedParameterValueException("TransformBefore Class", transferBefore, ex);
			}
		}

		String stringPayload;
		if (payload instanceof String) {
			stringPayload = (String) payload;
		} else {
			stringPayload = objectToJson(payload);
		}

		request.setEntity(new StringEntity(stringPayload, StandardCharsets.UTF_8));
	}

	public static UsernamePasswordCredentials basicAuthenticationToCredentials(String basicAuthorizationHeader) {
		if (StringUtils.isBlank(basicAuthorizationHeader)) {
			return null;
		}

		String base64BasicAuthorization = StringUtils.removeStartIgnoreCase(basicAuthorizationHeader, "Basic ");
		String basicAuthorization = base64decode(base64BasicAuthorization);
		if (basicAuthorization.contains(":")) {
			String[] credentials = StringUtils.split(basicAuthorization, ":");

			return new UsernamePasswordCredentials(credentials[0], credentials[1]);
		}

		return null;
	}

	public static String base64decode(String textToDecode) {
		if (textToDecode == null) {
			return null;
		}

		if (StringUtils.isBlank(textToDecode)) {
			return "";
		}

		return new String(DatatypeConverter.parseBase64Binary(textToDecode));
	}

	public static String base64encode(String user, String password) {
		return base64encode(MessageFormat.format("{0}:{1}", user, password));
	}

	private static String base64encode(String textToEncode) {
		return DatatypeConverter.printBase64Binary((textToEncode).getBytes());
	}

	public static String objectToJson(Object obj) throws SkyglassException {
		if (obj == null) {
			return null;
		}

		try {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
		} catch (JsonProcessingException ex) {
			throw new SkyglassException(ex);
		}
	}

	public static <T> T jsonToObject(Class<?> resourceClass, String jsonFile, Class<T> dtoClass) {
		URL url = resourceClass.getClassLoader().getResource(jsonFile);

		try {
			return mapper.readValue(new File(url.getFile()), dtoClass);
		} catch (Exception e) {
			log.error("Exception occurred while parsing json to object", e);
		}

		return null;
	}

	public static <T extends Object> T jsonToObject(String json, Class<T> type) throws JsonProcessingException {
		if (type == null || json == null) {
			return null;
		}

		return mapper.readValue(json, type);
	}

	public static <T extends Object> T jsonToObject(String json, TypeReference<T> type) throws JsonProcessingException {
		if (type == null || json == null) {
			return null;
		}

		return mapper.readValue(json, type);
	}

	public static <T extends Object> T convertValue(Object obj, Class<T> type) throws IllegalArgumentException {
		if (type == null || obj == null) {
			return null;
		}

		return mapper.convertValue(obj, type);
	}

	public static <T extends Object> T convertValue(Object obj, TypeReference<T> type) throws IllegalArgumentException {
		if (type == null || obj == null) {
			return null;
		}

		return mapper.convertValue(obj, type);
	}

	public static HttpServletRequest toHttpServletRequest(WebRequest webRequest) {
		if (webRequest instanceof ServletWebRequest) {
			return ((ServletWebRequest) webRequest).getRequest();
		}

		return null;
	}

	public static WebRequest toWebRequest(HttpServletRequest request) {
		if (request == null) {
			return null;
		}

		return new ServletWebRequest(request);
	}
}

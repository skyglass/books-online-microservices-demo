package skyglass.composer.product.exception;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.error.AbstractErrorController;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ErrorHandler extends AbstractErrorController {
	private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

	private static final String PATH = "/error";

	private final ErrorAttributes errorAttributes;

	@Autowired
	public ErrorHandler(ErrorAttributes errorAttributes) {
		super(errorAttributes);

		this.errorAttributes = errorAttributes;
	}

	@RequestMapping(value = PATH)
	public ResponseEntity<?> error(HttpServletRequest request) throws Throwable {
		Throwable t = getError(request);
		if (t == null) {
			t = new Exception("Unknown error occured");
		}

		log.error("ErrorHandler caught an error", t);

		throw t;
	}

	private Throwable getError(HttpServletRequest request) {
		return this.errorAttributes.getError(HttpClientUtil.toWebRequest(request));
	}

	@Override
	public String getErrorPath() {
		return PATH;
	}
}

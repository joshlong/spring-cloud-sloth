package org.springframework.cloud.sloth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * I'M NOT SORRY
 *
 * @author Josh Long
 */
@Configuration
@ConditionalOnWebApplication
public class SlothAutoConfiguration {

	@Bean
	ImageRestController slothImages() {
		return new ImageRestController();
	}

	@Bean
	SlothFilter sloths() {
		return new SlothFilter();
	}
}

/**
 * Returns a JSON feed for the JavaScript client. The feed simply enumerates all the
 * images in the {@code images} directory. We could in theory generate this feed from a
 * Google image search result..
 */
@RestController
class ImageRestController {

	@Autowired
	private ResourcePatternResolver patternResolver;

	@Value("${sloth.images.pattern:classpath:/META-INF/resources/images/*.jpg}")
	private String pattern;

	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, value = "/images.json")
	Collection<Map<String, String>> images(HttpServletRequest request) throws Exception {
		return this.resolveImages(request.getContextPath());
	}

	private List<Map<String, String>> resolveImages(String contextPath) throws Exception {
		Resource[] resources = this.patternResolver.getResources(this.pattern);
		return Arrays.asList(resources).stream()
				.map(resource -> "images/" + resource.getFilename())
				.map(resourceName -> Collections.singletonMap("uri",
						(contextPath + '/' + resourceName).replaceAll("//", "/")))
				.collect(Collectors.toList());
	}
}

/**
 * injects a script, {@code sloths.js}, which in turn loads a slew of other JavaScript and
 * CSS to create a carousel of the images available from the {@link ImageRestController}
 * REST endpoint above.
 */
class SlothFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
			FilterChain filterChain) throws IOException, ServletException {

		HtmlResponseWrapper capturingResponseWrapper = new HtmlResponseWrapper(
				(HttpServletResponse) servletResponse);

		filterChain.doFilter(servletRequest, capturingResponseWrapper);

		if (servletResponse.getContentType() != null
				&& servletResponse.getContentType().contains("text/html")) {
			String content = capturingResponseWrapper.getCaptureAsString();
			if (!content.contains("/sloths.js")) {
				String sleepyScript = String.format("<script src=\"%s\"></script>",
						servletRequest.getServletContext().getContextPath()
								+ "/sloths.js");
				String closeBodyTag = "</body>";
				int bodyStart = content.toLowerCase().lastIndexOf(closeBodyTag);
				String newHtml = content.substring(0, bodyStart) + sleepyScript
						+ content.substring(bodyStart);
				content = newHtml;
			}
			servletResponse.setContentLength(content.length());
			servletResponse.getWriter().write(content);
		}
		else {
			byte[] out = capturingResponseWrapper.getCaptureAsBytes();
			servletResponse.getOutputStream().write(out);
		}
	}

	@Override
	public void destroy() {
	}
}

/**
 * This code was <a href=
 * "http://www.leveluplunch.com/java/tutorials/034-modify-html-response-using-filter/">
 * taken from this very helpful blog post which also happens to use Spring Boot!</a>
 */
class HtmlResponseWrapper extends HttpServletResponseWrapper {

	private final ByteArrayOutputStream capture;
	private ServletOutputStream output;
	private PrintWriter writer;

	public HtmlResponseWrapper(HttpServletResponse response) {
		super(response);
		this.capture = new ByteArrayOutputStream(response.getBufferSize());
	}

	@Override
	public ServletOutputStream getOutputStream() {
		if (this.writer != null) {
			throw new IllegalStateException(
					"getWriter() has already been called on this response.");
		}

		if (this.output == null) {
			this.output = new ServletOutputStream() {
				@Override
				public void write(int b) throws IOException {
					HtmlResponseWrapper.this.capture.write(b);
				}

				@Override
				public void flush() throws IOException {
					HtmlResponseWrapper.this.capture.flush();
				}

				@Override
				public void close() throws IOException {
					HtmlResponseWrapper.this.capture.close();
				}

				@Override
				public boolean isReady() {
					return false;
				}

				@Override
				public void setWriteListener(WriteListener arg0) {
				}
			};
		}

		return this.output;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (this.output != null) {
			throw new IllegalStateException(
					"getOutputStream() has already been called on this response.");
		}

		if (this.writer == null) {
			OutputStreamWriter out = new OutputStreamWriter(this.capture,
					getCharacterEncoding());
			this.writer = new PrintWriter(out);
		}

		return this.writer;
	}

	@Override
	public void flushBuffer() throws IOException {
		super.flushBuffer();

		if (this.writer != null) {
			this.writer.flush();
		}
		else if (this.output != null) {
			this.output.flush();
		}
	}

	public byte[] getCaptureAsBytes() throws IOException {
		if (this.writer != null) {
			this.writer.close();
		}
		else if (this.output != null) {
			this.output.close();
		}
		return this.capture.toByteArray();
	}

	public String getCaptureAsString() throws IOException {
		return new String(getCaptureAsBytes(), getCharacterEncoding());
	}

}
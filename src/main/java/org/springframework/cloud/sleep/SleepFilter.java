package org.springframework.cloud.sleep;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * I'M NOT SORRY
 *
 * @author Josh Long
 */
public class SleepFilter implements Filter {


    private Log log = LogFactory.getLog(getClass());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }


    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {

        HtmlResponseWrapper capturingResponseWrapper =
                new HtmlResponseWrapper((HttpServletResponse) servletResponse);

        filterChain.doFilter(servletRequest, capturingResponseWrapper);
        if (servletResponse.getContentType() != null && servletResponse.
                getContentType().contains("text/html")) {
            String content = capturingResponseWrapper.getCaptureAsString();
            String sleepyScript = String.format("<script src='%s'></script>",
                    servletRequest.getServletContext().getContextPath() + "/sleepy.js");
            String closeBodyTag = "</body>";
            int bodyStart = content.toLowerCase().lastIndexOf(closeBodyTag);
            String newHtml = content.substring(0, bodyStart)
                    + sleepyScript
                    + "</body>"
                    + content.substring(bodyStart + closeBodyTag.length());
            log.info(newHtml);
            servletResponse.getWriter().write(newHtml);
            servletResponse.setContentLength(newHtml.length());

        } else {
            byte[] out = capturingResponseWrapper.getCaptureAsBytes();
            servletResponse.getOutputStream().write(out);
        }
    }

    @Override
    public void destroy() {
    }


    /**
     * This code was <a href="http://www.leveluplunch.com/java/tutorials/034-modify-html-response-using-filter/">
     * taken from this very helpful blog post which also happens to use Spring Boot!</a>
     */
    static class HtmlResponseWrapper extends HttpServletResponseWrapper {

        private final ByteArrayOutputStream capture;
        private ServletOutputStream output;
        private PrintWriter writer;

        public HtmlResponseWrapper(HttpServletResponse response) {
            super(response);
            capture = new ByteArrayOutputStream(response.getBufferSize());
        }

        @Override
        public ServletOutputStream getOutputStream() {
            if (writer != null) {
                throw new IllegalStateException(
                        "getWriter() has already been called on this response.");
            }

            if (output == null) {
                output = new ServletOutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        capture.write(b);
                    }

                    @Override
                    public void flush() throws IOException {
                        capture.flush();
                    }

                    @Override
                    public void close() throws IOException {
                        capture.close();
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

            return output;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (output != null) {
                throw new IllegalStateException(
                        "getOutputStream() has already been called on this response.");
            }

            if (writer == null) {
                writer = new PrintWriter(new OutputStreamWriter(capture,
                        getCharacterEncoding()));
            }

            return writer;
        }

        @Override
        public void flushBuffer() throws IOException {
            super.flushBuffer();

            if (writer != null) {
                writer.flush();
            } else if (output != null) {
                output.flush();
            }
        }

        public byte[] getCaptureAsBytes() throws IOException {
            if (writer != null) {
                writer.close();
            } else if (output != null) {
                output.close();
            }

            return capture.toByteArray();
        }

        public String getCaptureAsString() throws IOException {
            return new String(getCaptureAsBytes(), getCharacterEncoding());
        }

    }

}
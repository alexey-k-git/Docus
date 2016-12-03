package ru.restfulrobot.docus.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.types.ObjectId;

import ru.restfulrobot.docus.DocusServer;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class WelcomeServlet extends HttpServlet {

    private static final long serialVersionUID = 7030951177346647203L;

    private Template welcome;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!"/".equals(req.getServletPath())) {
            getResource(req, resp);
            return;
        }
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("root", new ObjectId().toString());
        try {
            Writer writer = new OutputStreamWriter(resp.getOutputStream());
            resp.addHeader("Content-Type", "text/html;charset=UTF-8");
            resp.addHeader("Transfer-Encoding", "chunked");
            getWelcomeTemplate().process(values, writer);
            writer.flush();
        } catch (TemplateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Template getWelcomeTemplate() {
        if (welcome == null) {
            try {
                welcome = DocusServer.getConfiguration().getTemplate("welcome.ftl");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return welcome;
    }

    private final static Map<String, String> MIME_TYPES = new HashMap<String, String>();

    static {
        MIME_TYPES.put("json", "text/css");
        MIME_TYPES.put("css", "text/css");
        MIME_TYPES.put("png", "text/png");
        MIME_TYPES.put("gif", "text/gif");
        MIME_TYPES.put("jpeg", "text/jpeg");
        MIME_TYPES.put("jpg", "text/jpeg");
        MIME_TYPES.put("js", "application/x-javascript;charset=utf-8");
    }

    private void getResource(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        try {
            InputStream resource = getResource(path.substring(1));
            if (resource != null) {
                String ext = path.substring(path.lastIndexOf(".") + 1);
                String ct = MIME_TYPES.get(ext);
                if (ct == null) {
                    ct = "text/plain";
                }
                resp.setContentType(ct);
                copyStream(resource, resp.getOutputStream());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private InputStream getResource(String resource) {
        return this.getClass().getClassLoader().getResourceAsStream(resource);
    }

    private static void copyStream(InputStream is, OutputStream os)
            throws IOException {
        byte[] buf = new byte[1024];
        int i = 0;
        while ((i = is.read(buf)) != -1) {
            os.write(buf, 0, i);
        }
        os.flush();
    }
}

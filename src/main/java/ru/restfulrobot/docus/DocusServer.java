package ru.restfulrobot.docus;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.http.Part;

import org.bson.types.ObjectId;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.JSONException;
import org.json.JSONObject;

import ru.restfulrobot.docus.DocTree.Document;
import ru.restfulrobot.docus.servlet.DocumentChildrenServlet;
import ru.restfulrobot.docus.servlet.DocumentOperation;
import ru.restfulrobot.docus.servlet.WelcomeServlet;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import com.mongodb.util.JSON;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class DocusServer {

    public static void main(String[] args) throws IOException {
        config = new Configuration();
        config.setClassForTemplateLoading(DocusServer.class, "/");
        new DocusServer().init();
    }

    private static Configuration config;

    public DocusServer() {
    }

    public static Configuration getConfiguration() {
        return config;
    }

    private void init() throws IOException {
        DocusRegistry.instance.initDbConnection();
        DocusRegistry.instance.initDocTree();
        initWebServer();
        addResources();
        addWelcomePage();
        childs();
        docShow();
        docCreate();
        docChange();
        idCreate();
        docMove();
        deleteDoc();
        uploadFile();
        deleteFile();
        docFind();
        downloadFile();
    }

    private void childs() {

        Spark.get("/childs", (request, response)-> {
                response.raw().setContentType("application/json;charset=utf-8");
                String nodeId = request.queryParams("id");
                final BasicDBObject query = new BasicDBObject();
                if (nodeId == null || "root".equals(nodeId)) {
                    query.append("parent", new BasicDBObject("$exists", 0));
                } else {
                    query.append("parent", new ObjectId(nodeId));
                }
                DBCursor childs = DocusRegistry.instance.getCollectionDoc()
                        .find(query);
                try {
                    Writer writer = new BufferedWriter(new OutputStreamWriter(
                            response.raw().getOutputStream()));
                    writer.write("[");
                    boolean notFirst = false;
                    while (childs.hasNext()) {
                        if (notFirst) {
                            writer.write(",");
                        }
                        writer.write(JSON.serialize(toDocumentNode(childs
                                .next())));
                        notFirst = true;
                    }
                    writer.write("]");
                    writer.flush();
                    response.status(200);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;

        });
    }

    private void docFind() {

        Spark.post("/find", (request, response)->{

                final String typeQuery = request.queryParams("queryType");
                final String textQuery = request.queryParams("queryText");
                ArrayList<Document> docs;
                switch (typeQuery) {

                    case "name":
                        docs = (ArrayList<Document>) DocusRegistry.instance
                                .getDocTree().findByName(textQuery);
                        break;

                    case "author":
                        docs = (ArrayList<Document>) DocusRegistry.instance
                                .getDocTree().findByAuthor(textQuery);
                        break;

                    case "tags":
                        docs = (ArrayList<Document>) DocusRegistry.instance
                                .getDocTree().findTags(textQuery);
                        break;

                    default:
                        docs = (ArrayList<Document>) DocusRegistry.instance
                                .getDocTree().findByName(textQuery);
                        break;
                }
                Document currentDoc;
                String result = "";
                int i = 0;
                while (i < docs.size()) {
                    currentDoc = docs.get(i);
                    result += currentDoc.getName() + "  "
                            + currentDoc.getAuthor() + "  "
                            + currentDoc.getModified().toString() + "  "
                            + currentDoc.getFile().toString() + "\n";
                    i++;
                }
                return result;
            }
        );
    }

    private void docShow() {
        Spark.post("/show", (request, response)->{
                final String currentDocumentId = request.queryParams("id");
                ObjectId realId = new ObjectId(currentDocumentId);
                if (DocusRegistry.instance.getDocTree().getMainNode().getId()
                        .equals(realId)) {
                    return "-";
                }

                Document currentDoc = DocusRegistry.instance.getDocTree()
                        .findByID(realId);
                String tags = "";
                for (int i = 0; i < currentDoc.getTags().size(); i++) {
                    tags += currentDoc.getTags().get(i);
                    if (i + 1 != currentDoc.getTags().size())
                        tags += ", ";
                }
                JSONObject obj = new JSONObject();
                try {
                    obj.put("name", currentDoc.getName());
                    obj.put("author", currentDoc.getAuthor());
                    obj.put("tags", tags);
                    obj.put("created", currentDoc.getCreated());
                    obj.put("modified", currentDoc.getModified());
                    obj.put("file", currentDoc.getFile());
                    if (currentDoc.getFile()) {

                        obj.put("fileName", currentDoc.getFileName());

                    } else {
                        obj.put("fileName", "Файл отсутствует");

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return obj;
            });

    }

    private void deleteFile() {
        Spark.post("/deleteFile", (request, response)->{
                final String currentDocumentId = request.queryParams("id");
                ObjectId realId = new ObjectId(currentDocumentId);
                DocTree docTree = DocusRegistry.instance.getDocTree();
                String filename = docTree.findByID(realId).getFileName();
                DocusRegistry.instance.getGridFS("DocFiles").remove(
                        DocusRegistry.instance.getGridFS("DocFiles").findOne(
                                realId));
                return filename;
            });
    }

    private void uploadFile() {
        Spark.post("/upload", (request, response)->{
                Part filePart = null;
                System.out.println(request.body());
                String currentDocumentId = null;
                try {
                    filePart = request.raw().getPart("docFile");
                } catch (IOException | ServletException e1) {
                    e1.printStackTrace();
                }
                InputStream filecontent = null;
                try {
                    filecontent = filePart.getInputStream();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                ObjectId realId = new ObjectId(currentDocumentId);
                Document currentDoc = DocusRegistry.instance.getDocTree()
                        .findByID(realId);
                GridFS gfsDoc = DocusRegistry.instance.getGridFS("DocFiles");
                GridFSInputFile gfsFile = null;
                gfsFile = gfsDoc.createFile(filecontent);
                gfsFile.setFilename("blablabal");
                gfsFile.setId(realId);
                gfsFile.save();
                currentDoc.setFile(true, "blablabal");
                return "??";
            });
    }

    private void downloadFile() {
        Spark.post("/download", (request, response)->{
                final String currentDocumentId = request.queryParams("id");
                ObjectId realId = new ObjectId(currentDocumentId);
                GridFS gfsDoc = DocusRegistry.instance.getGridFS("DocFiles");
                GridFSDBFile fileForOutput = gfsDoc.findOne(realId);
                ;
                response.header("Content-Description", "File Transfer");
                response.header("Content-Type", "application/octet-stream");
                InputStream resource = fileForOutput.getInputStream();
                try {
                    copyStream(resource, response.raw().getOutputStream());
                } catch (IOException e) {

                    e.printStackTrace();
                }
                response.status(200);
                return null;

            });
    }

    private void docCreate() {

        Spark.post("/create", (request, response)->{
                final String parentId = request.queryParams("idParent");
                ObjectId objectIdParent = new ObjectId(parentId);
                final String name = request.queryParams("name");
                final String author = request.queryParams("author");
                final String tags = request.queryParams("tags");
                DocTree docTree = DocusRegistry.instance.getDocTree();
                Document newdoc = docTree.createDocument(name, author);
                Scanner s = new Scanner(tags);
                s.useDelimiter(",\\s*");
                while (s.hasNext()) {
                    newdoc.getTags().add(s.next());
                }
                s.close();
                if (docTree.getMainNode().getId().equals(objectIdParent)) {
                    docTree.getMainNode().insert(newdoc, -1);
                } else {
                    Document currentDoc = docTree.findByID(objectIdParent);
                    currentDoc.insert(newdoc, -1);
                }
                newdoc.setFile(false);
                String newString = newdoc.getId().toString();

                return newString;

            });
    }

    private void docChange() {

        Spark.post("/change",(request, response)->{
                final String docId = request.queryParams("id");
                ObjectId realId = new ObjectId(docId);
                final String name = request.queryParams("name");
                final String author = request.queryParams("author");
                final String tags = request.queryParams("tags");
                Document currentDoc = DocusRegistry.instance.getDocTree()
                        .findByID(realId);
                currentDoc.getTags().clear();
                Scanner s = new Scanner(tags);
                s.useDelimiter(",\\s*");
                while (s.hasNext()) {
                    currentDoc.getTags().add(s.next());
                }
                s.close();
                currentDoc.setName(name);
                currentDoc.setAuthor(author);
                currentDoc.setModified(new Date());
                currentDoc.update();
                String newString = currentDoc.getId().toString();
                return newString;
            });
    }

    private void idCreate() {

        Spark.post("/getnewid",(request, response)->{
                ObjectId id = new ObjectId();
                return id.toString();

            });
    }

    private void deleteDoc() {

        Spark.post("/delete", (request, response)->{
                final String currentDocumentId = request.queryParams("id");
                ObjectId realId = new ObjectId(currentDocumentId);

                Document currentDoc = DocusRegistry.instance.getDocTree()
                        .findByID(realId);
                Integer positionSelectedDoc = currentDoc.getPosition();
                if (currentDoc.getFile()) {
                    try {
                        GridFS gfsDoc = DocusRegistry.instance
                                .getGridFS("DocFiles");
                        gfsDoc.remove(gfsDoc.findOne(realId));
                    } catch (MongoException e) {
                        e.printStackTrace();
                    }
                }
                DocusRegistry.instance.getDocTree().reOrder(null,
                        currentDoc.getParent(), positionSelectedDoc - 1);
                currentDoc.removeFromParent();
                return "deleted";
            });
    }

    private void docMove() {
        Spark.post("/move",(request, response)->{
                final String selected = request.queryParams("selectedId");
                final String destination = request.queryParams("destinationId");
                final String option = request.queryParams("option");
                ObjectId selectedId = new ObjectId(selected);
                ObjectId destinationId = new ObjectId(destination);
                DocTree docTree = DocusRegistry.instance.getDocTree();
                Document selectedDoc = docTree.findByID(selectedId);
                Document destinationDoc;
                if (docTree.getMainNode().getId().equals(destinationId)) {
                    destinationDoc = docTree.getMainNode();
                } else {
                    destinationDoc = docTree.findByID(destinationId);
                }
                Integer positionSelectedDoc = selectedDoc.getPosition();
                Document parentSelectedDocument = selectedDoc.getParent();
                switch (option) {
                    case "last": {
                        destinationDoc.insert(selectedDoc, -1);
                        docTree.reOrder(null, parentSelectedDocument,
                                positionSelectedDoc - 1);
                        selectedDoc.setPosition(destinationDoc.getChildCount() - 1);
                    }
                    break;
                    case "before": {
                        Integer positionDestinationDoc = destinationDoc
                                .getPosition();
                        destinationDoc.getParent().insert(selectedDoc,
                                positionDestinationDoc);
                        docTree.reOrder(null, parentSelectedDocument,
                                positionSelectedDoc - 1);

                        docTree.reOrder(destinationDoc, null,
                                positionDestinationDoc);
                        selectedDoc.setPosition(positionDestinationDoc);

                    }
                    break;
                    case "after": {
                        Integer positionDestinationDoc;
                        if (parentSelectedDocument.equals(destinationDoc
                                .getParent())
                                && destinationDoc.getPosition() < selectedDoc
                                .getPosition()) {

                            positionDestinationDoc = destinationDoc.getPosition() + 1;

                        } else {
                            positionDestinationDoc = destinationDoc.getPosition();
                        }

                        destinationDoc.getParent().insert(selectedDoc,
                                positionDestinationDoc);
                        docTree.reOrder(null, parentSelectedDocument,
                                positionSelectedDoc - 1);

                        docTree.reOrder(destinationDoc, null,
                                positionDestinationDoc);
                        selectedDoc.setPosition(positionDestinationDoc);

                    }
                    break;
                }
                return null;

            });
    }

    public static BasicDBObject toDocumentNode(DBObject object) {
        DocTree.Document doc = DocusRegistry.instance.getDocTree().new Document(
                null, object);
        BasicDBObject map = new BasicDBObject("attr", new BasicDBObject("id",
                doc.id.toString()).append("rel", "document")).append("data",
                doc.getName());
        DBObject child = DocusRegistry.instance.getCollectionDoc().findOne(
                new BasicDBObject("parent", doc.id));
        if (child == null) {
            map.append("state", "");
        } else {
            map.append("state", "closed");
        }
        return map;
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

    private void addResources() {
        Spark.get("/js/*",(request, response)->{
                String path = request.pathInfo();
                try {
                    InputStream resource = getResource(path.substring(1));
                    if (resource != null) {
                        String ext = path.substring(path.lastIndexOf(".") + 1);
                        String ct = MIME_TYPES.get(ext);
                        if (ct == null) {
                            ct = "text/plain";
                        }
                        response.raw().setContentType(ct);
                        copyStream(resource, response.raw().getOutputStream());
                        response.status(200);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            });
    }

    private void addWelcomePage() throws IOException {
        final Template welcome = config.getTemplate("welcome.ftl");
        Spark.get("/",(request, response)->{
                response.header("Content-Type", "text/html;charset=UTF-8");
                response.header("Transfer-Encoding", "chunked");
                Map<String, Object> values = new HashMap<String, Object>();
                values.put("root", new ObjectId().toString());
                try {
                    Writer writer = new OutputStreamWriter(response.raw()
                            .getOutputStream());
                    welcome.process(values, writer);
                    writer.flush();
                    response.status(200);
                } catch (TemplateException | IOException e) {
                    e.printStackTrace();
                }
                return null;
            });
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

    private Server initWebServer() throws IOException {
        try {
            Server server = new Server(8080);
            ServletContextHandler context = new ServletContextHandler(
                    ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            context.addServlet(new ServletHolder(new WelcomeServlet()), "/");
            context.addServlet(new ServletHolder(new DocumentChildrenServlet()), "/childs");
            context.addServlet(new ServletHolder(new DocumentOperation("show")), "/show");
            context.addServlet(new ServletHolder(new DocumentOperation("create")), "/create");
            context.addServlet(new ServletHolder(new DocumentOperation("change")), "/change");
            context.addServlet(new ServletHolder(new DocumentOperation("delete")), "/delete");
            context.addServlet(new ServletHolder(new DocumentOperation("move")), "/move");
            context.addServlet(new ServletHolder(new DocumentOperation("upload")), "/upload");
            context.addServlet(new ServletHolder(new DocumentOperation("deletefile")), "/deletefile");
            context.addServlet(new ServletHolder(new DocumentOperation("download")), "/download");
            context.addServlet(new ServletHolder(new DocumentOperation("find")), "/find");
            context.addServlet(new ServletHolder(new DocumentOperation("login")), "/login");
            server.setHandler(context);
            server.start();
            System.out.println("== Jetty has ignited ...\n>> Listening on 0.0.0.0:8080");
            return server;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to configure Jetty server", e);
        }

    }
}
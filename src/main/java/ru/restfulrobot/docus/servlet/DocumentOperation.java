package ru.restfulrobot.docus.servlet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.restfulrobot.docus.DocTree;
import ru.restfulrobot.docus.DocTree.Document;
import ru.restfulrobot.docus.DocusApplication;
import ru.restfulrobot.docus.DocusRegistry;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.MongoException;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

public class DocumentOperation extends JsonServlet {

    /** Unique Id */
    private static final long serialVersionUID = 5526704418328894690L;

    String operation;

    public DocumentOperation(String operation) {
        super();
        this.operation = operation;
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html;charset=utf-8");

        switch (operation) {
            case "show":
                doShow(request, response);
                break;
            case "create":
                doCreate(request, response);
                break;
            case "change":
                doUpdate(request, response);
                break;
            case "delete":
                doRemove(request);
                break;
            case "move":
                doMove(request);
                break;
            case "upload":
                doUpload(request, response);
                break;
            case "deletefile":
                doDeleteFile(request, response);
                break;
            case "download":
                doDownload(request, response);
                break;
            case "login":
                doLogin(request, response);
                break;
            case "find":
                doFind(request, response);
                break;
        }
    }

    private void doFind(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String typeQuery = request.getParameter("queryType");
        final String textQuery = request.getParameter("queryText");
        Collection<Document> docs;
        DocTree tree = DocusRegistry.instance
                .getDocTree();
        switch (typeQuery) {

            case "name":
                docs = tree.findByName(textQuery);
                break;

            case "author":
                docs = tree.findByAuthor(textQuery);
                break;

            case "tags":
                docs = tree.findTags(textQuery);
                break;

            default:
                docs = tree.findByName(textQuery);
                break;
        }
        try {
            JSONObject object = new JSONObject();
            JSONArray items = new JSONArray();
            for (Document currentDoc : docs) {
                JSONObject doc = new JSONObject();
                doc.put("name", currentDoc.getName());
                doc.put("author", currentDoc.getAuthor());
                doc.put("modified", currentDoc.getModified());
                doc.put("file", (currentDoc.getFile()) ? "Присутствует" : "Отсутствует");
                doc.put("id", currentDoc.getId());
                StringBuilder pathToDocument = new StringBuilder();
                List<ObjectId> ancestorsId = currentDoc.getAncestorsId();
                for (int j = 0; j < ancestorsId.size(); j++) {
                    pathToDocument.append(ancestorsId.get(j)).append("-");
                }
                pathToDocument.append(currentDoc.getId());
                doc.put("path", pathToDocument.toString());

                items.put(doc);
            }
            object.put("size", docs.size());
            object.put("items", items);
            object.write(response.getWriter());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void doLogin(HttpServletRequest request, HttpServletResponse response) throws UnknownHostException, IOException {
        String login = request.getParameter("login");
        String password = request.getParameter("password");
        DocusRegistry.instance.initDbConnection();
        DBCollection users = DocusRegistry.instance.getCollectionUser();
        BasicDBObject user = (BasicDBObject) users
                .findOne(new BasicDBObject("_id", login));
        try {
            JSONObject obj = new JSONObject();
            if (user == null) {

                obj.put("result", "noFound");

            }
            else if (user.getString("password").equals(DocusApplication.encodePassword(password, "DocusForever"))) {
                StringBuilder role = new StringBuilder();
                BasicDBList roleList = (BasicDBList) user.get("roles");
                if (roleList!=null) {
                    for (Object o : roleList) {
                        role.append(o);
                    }
                    obj.put("role", role.toString());
                }
                else
                {
                    obj.put("role","admin");
                }
                obj.put("result", "Found");
                obj.put("name", login);

            } else {
                obj.put("result", "passwordError");
            }
            obj.write(response.getWriter());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void doDownload(HttpServletRequest request, HttpServletResponse response) throws UnknownHostException, IOException, FileNotFoundException {
        DocusRegistry.instance.initDbConnection();
        DocTree docTree = DocusRegistry.instance.getDocTree();
        String currentDocumentId = request.getParameter("id");
        ObjectId realId = new ObjectId(currentDocumentId);
        Document doc = docTree.findByID(realId);
        GridFSDBFile fileForSave = DocusRegistry.instance.getGridFS(
                "DocFiles").findOne(realId);
        // response.getWriter().write(file.getAbsolutePath());
        /*
         * ServletOutputStream fileOutputStream =
         * response.getOutputStream();
         * response.setContentType("application/octet-stream");
         * response.setHeader("Content-Disposition",
         * "attachment; filename=\"" + fileName + "\""); int read = 0; while
         * ((read = fileInputStream.read()) != -1) {
         * fileOutputStream.write(read); } fileOutputStream.flush();
         * fileOutputStream.close(); fileInputStream.close();
         */
        // String fileName = request.getParameter("filename");

        response.setContentType("application/octet-stream");
        response.setContentLength((int) fileForSave.getLength());
        response.setHeader("Content-Transfer-Encoding", "binary");
        response.setHeader("Content-Disposition", "attachment; filename=\""
                + doc.getFileName() + "\"");

        ServletOutputStream os = response.getOutputStream();
        fileForSave.writeTo(os);
        os.close();
    }

    private void doDeleteFile(HttpServletRequest request, HttpServletResponse response) throws UnknownHostException, IOException {
        DocusRegistry.instance.initDbConnection();
        DocTree docTree = DocusRegistry.instance.getDocTree();
        String currentDocumentId = request.getParameter("id");
        ObjectId realId = new ObjectId(currentDocumentId);
        Document node = docTree.findByID(realId);
        String filename = node.getFileName();
        DocusRegistry.instance.getGridFS("DocFiles").remove(
                DocusRegistry.instance.getGridFS("DocFiles")
                        .findOne(realId));
        node.setFile(false);
        response.getWriter().write(filename);
    }

    private void doUpload(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        try {
            DocusRegistry.instance.initDbConnection();
            DocTree docTree = DocusRegistry.instance.getDocTree();
            List<FileItem> items = new ServletFileUpload(
                    new DiskFileItemFactory()).parseRequest(request);
            String id = getItemValue("id", items).getString();
            Document node = docTree.findByID(new ObjectId(id));
            FileItem file = getItemValue("pathfile", items);
            String fileName = file.getName();
            GridFSInputFile gfsFile = DocusRegistry.instance.getGridFS(
                    "DocFiles").createFile(file.getInputStream());
            gfsFile.setFilename(fileName);
            gfsFile.setId(node.getId());
            gfsFile.save();
            node.setFile(true, fileName);
            JSONObject obj = new JSONObject();
            obj.put("file", node.getFile());
            obj.put("fileName", node.getFileName());
            obj.put("modified", node.getModified());
            obj.write(response.getWriter());

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileUploadException e) {
            e.printStackTrace();
            throw new ServletException("Cannot parse multipart request.", e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Cannot parse request.", e);
        }
    }

    private void doMove(HttpServletRequest request) {
        final String selected = request.getParameter("selectedId");
        final String destination = request.getParameter("destinationId");
        final String option = request.getParameter("option");
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
                Integer positionDestinationDoc = destinationDoc.getPosition();
                destinationDoc.getParent().insert(selectedDoc,
                        positionDestinationDoc);
                docTree.reOrder(null, parentSelectedDocument,
                        positionSelectedDoc - 1);
                docTree.reOrder(destinationDoc, null, positionDestinationDoc);
                selectedDoc.setPosition(positionDestinationDoc);

            }
            break;
            case "after": {
                Integer positionDestinationDoc;
                if (parentSelectedDocument.equals(destinationDoc.getParent())
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

                docTree.reOrder(destinationDoc, null, positionDestinationDoc);
                selectedDoc.setPosition(positionDestinationDoc);

            }
            break;
        }
    }

    private void doRemove(HttpServletRequest request) {
        final String currentDocumentId = request.getParameter("id");
        ObjectId realId = new ObjectId(currentDocumentId);

        Document currentDoc = DocusRegistry.instance.getDocTree().findByID(
                realId);
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
    }

    private void doUpdate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String docId = request.getParameter("id");
        ObjectId realId = new ObjectId(docId);
        final String name = request.getParameter("name");
        final String author = request.getParameter("author");
        final String tags = request.getParameter("tags");
        Document currentDoc = DocusRegistry.instance.getDocTree().findByID(
                realId);
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
        response.getWriter().write(newString);
    }

    private void doCreate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String parentId = request.getParameter("idParent");
        ObjectId objectIdParent = new ObjectId(parentId);
        final String name = request.getParameter("name");
        final String author = request.getParameter("author");
        final String tags = request.getParameter("tags");
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
        response.getWriter().write(newString);
    }

    private void doShow(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String currentDocumentId = request.getParameter("id");
        ObjectId realId = new ObjectId(currentDocumentId);

        if (DocusRegistry.instance.getDocTree().getMainNode().getId().equals(realId)) {
            response.getWriter().write("-");
            return;
        }
        Document currentDoc = DocusRegistry.instance.getDocTree().findByID(realId);
        StringBuilder tags = new StringBuilder();
        for (String tag : currentDoc.getTags()) {
            tags.append(tag).append(",");
        }
        if (tags.length() > 0) {
            // remove last comma
            tags.setLength(tags.length() - 1);
        }
        try {
            JSONObject obj = new JSONObject();
            obj.put("name", currentDoc.getName());
            obj.put("author", currentDoc.getAuthor());
            obj.put("tags", tags.toString());
            obj.put("created", currentDoc.getCreated());
            obj.put("modified", currentDoc.getModified());
            obj.put("file", currentDoc.getFile());
            obj.put("fileName", currentDoc.getFile() ? currentDoc.getFileName() : "-");
            obj.write(response.getWriter());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private FileItem getItemValue(String name, List<FileItem> items) {
        for (FileItem item : items) {
            if (name.equals(item.getFieldName())) {
                return item;
            }
        }
        return null;
    }

}

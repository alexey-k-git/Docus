package ru.restfulrobot.docus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.bson.types.ObjectId;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

public class XMLCreator {
    DocumentBuilderFactory factory;
    DocumentBuilder builder;
    Document document;
    Element mainNode;
    Element doc;
    Element childs;
    Element name;
    Element author;
    Element tags;
    Element dateCreated;
    Element dateChanged;
    Element id;
    Element file;
    Element fileName;
    Element parent;
    Element order;
    Element department;
    Element changes;
    Element change;
    Element changeUser;
    Element changeDepartment;
    Element changeDate;
    Element changeFile;
    String tagsInOneString;

    public Boolean createXMLDocument(
            Collection<ru.restfulrobot.docus.DocTree.Document> documents,
            DocusFrame mainwindow, GridFS gfsDoc) {
        try {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new XmlFileFilter());
            fc.setAcceptAllFileFilterUsed(false);
            int returnVal = fc.showSaveDialog(mainwindow);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                initXML();
                for (ru.restfulrobot.docus.DocTree.Document currentDoc : documents) {
                    fillXmlDocumentAndCreateFolderwithFiles(currentDoc, fc,
                            gfsDoc);
                }
                saveXML(fc);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Boolean loadDBfromXML(DocusFrame mainwindow, DBCollection docs,
                                 GridFS gfsDoc) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new XmlFileFilter());
        fc.setAcceptAllFileFilterUsed(false);
        int returnVal = fc.showOpenDialog(mainwindow);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            Object[] options = { "Ок", "Отмена" };
            int n;
            n = JOptionPane
                    .showOptionDialog(
                            mainwindow,
                            "Вы действительно хотите заменить вашу текущую БД новой БД из файла?",
                            "Предупреждение", JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE, null, options,
                            options[1]);
            if (n == 0) {
                docs.drop();
                gfsDoc.getDB().getCollection("DocFiles.files").drop();
                gfsDoc.getDB().getCollection("DocFiles.chunks").drop();
                File file = fc.getSelectedFile();
                factory = DocumentBuilderFactory.newInstance();
                try {
                    builder = factory.newDocumentBuilder();
                    document = builder.parse(file);
                    document.normalize();
                    NodeList listOfDocuments = document
                            .getElementsByTagName("Document");
                    int totalDocuments = listOfDocuments.getLength();
                    for (int s = 0; s < totalDocuments; s++) {
                        Element currentDocument = (Element) listOfDocuments
                                .item(s);
                        saveNodeInDB(currentDocument, gfsDoc, file, docs);
                    }
                    return true;
                } catch (ParserConfigurationException  e) {
                    e.printStackTrace();
                }
                catch (SAXException e)
                {
                    e.printStackTrace();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            } else
                return false;
        }
        return false;
    }

    private void initXML() {

        factory = DocumentBuilderFactory.newInstance();
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        document = builder.newDocument();
        document.setXmlStandalone(true);
        document.setNodeValue("Docus");
        doc = document.createElement("Document");
        name = document.createElement("Name");
        mainNode = document.createElement("Docus");
        document.appendChild(mainNode);

    }

    private void fillXmlDocumentAndCreateFolderwithFiles(
            ru.restfulrobot.docus.DocTree.Document currentDoc, JFileChooser fc,
            GridFS gfsDoc) throws IOException {
        doc = document.createElement("Document");
        name = document.createElement("Name");
        author = document.createElement("Author");
        tags = document.createElement("Tags");
        file = document.createElement("File");
        order = document.createElement("Order");
        department = document.createElement("Department");
        dateCreated = document.createElement("DateCreated");
        dateChanged = document.createElement("DateChanged");
        changes = document.createElement("Changes");
        doc.setAttribute("id", currentDoc.getId().toString());
        name.setTextContent(currentDoc.getName());
        author.setTextContent(currentDoc.getAuthor());
        tagsInOneString = new String("");
        for (int i = 0; i < currentDoc.getTags().size(); i++) {
            tagsInOneString += currentDoc.getTags().get(i);
            if (i + 1 != currentDoc.getTags().size())
                tagsInOneString += ", ";
        }
        tags.setTextContent(tagsInOneString);
        dateCreated.setTextContent(Long.toString(currentDoc.getCreated()
                .getTime()));
        dateChanged.setTextContent(Long.toString(currentDoc.getModified()
                .getTime()));
        department.setTextContent(currentDoc.getDepartment());
        mainNode.appendChild(doc);
        doc.appendChild(name);
        doc.appendChild(author);
        doc.appendChild(tags);
        doc.appendChild(dateChanged);
        doc.appendChild(dateCreated);
        doc.appendChild(department);
        doc.appendChild(changes);
        if (currentDoc.getParent() == null) {
            return;
        }
        doc.setAttribute("parent", currentDoc.getParent().getId().toString());
        order.setTextContent(currentDoc.getPosition().toString());
        doc.appendChild(order);
        writeChanges(changes, currentDoc);
        File dir = fc.getSelectedFile();
        if (dir.getPath().indexOf(".xml") != -1) {
            dir = new File(dir.getAbsolutePath().replaceAll(".xml", ""));
        }
        dir.mkdir();
        if (currentDoc.getFile()) {
            file.setTextContent("yes");
            fileName = document.createElement("FileName");
            fileName.setTextContent(currentDoc.getFileName());
            doc.appendChild(file);
            doc.appendChild(fileName);
            String pathForDocumentFile = dir.getAbsolutePath() + "/"
                    + currentDoc.getId().toString();
            File documentFile = new File(pathForDocumentFile);
            GridFSDBFile fileFromMongoDB = gfsDoc.findOne(currentDoc.getId());
            fileFromMongoDB.writeTo(documentFile);
        } else {
            doc.appendChild(file);
            file.setTextContent("no");
        }
    }

    private void writeChanges(Element changes,
                              ru.restfulrobot.docus.DocTree.Document currentDoc) {
        Stack<DocusChangeDocument> changesStack = currentDoc.getChanges();
        String file;
        for (DocusChangeDocument currentChange : changesStack) {
            change = document.createElement("Change");
            changeUser = document.createElement("User");
            changeDepartment = document.createElement("Department");
            changeDate = document.createElement("Date");
            changeFile = document.createElement("File");
            changeUser.setTextContent(currentChange.getName());
            changeDepartment.setTextContent(currentChange.getDepartment());
            changeDate.setTextContent(Long.toString(currentChange.getDate()
                    .getTime()));
            file = currentChange.getChangeFile() ? "yes" : "no";
            changeFile.setTextContent(file);
            change.appendChild(changeUser);
            change.appendChild(changeDepartment);
            change.appendChild(changeDate);
            change.appendChild(changeFile);
            changes.appendChild(change);
        }

    }

    private void saveXML(JFileChooser fc) {
        TransformerFactory transFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = transFactory.newTransformer();
        } catch (TransformerConfigurationException e1) {
            e1.printStackTrace();
        }
        DOMSource source = new DOMSource(document);
        File fileFromDialog = fc.getSelectedFile();
        String fileInXML = fileFromDialog.getAbsolutePath() + ".xml";
        fileFromDialog = new File(fileInXML);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fileFromDialog);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        StreamResult result = new StreamResult(fos);
        try {
            transformer.transform(source, result);
        } catch (TransformerException e) {
            e.printStackTrace();
        }

    }

    private void saveNodeInDB(Element currentDocument, GridFS gfsDoc,
                              File file, DBCollection docs) {
        File currentFile;
        String docId = currentDocument.getAttributeNode("id").getNodeValue();
        NodeList docParams = currentDocument.getChildNodes();
        String docName = ((Element) docParams.item(0)).getTextContent();
        String docAuthor = ((Element) docParams.item(1)).getTextContent();
        String docTagsOneString = ((Element) docParams.item(2))
                .getTextContent();
        List<String> docTags = new ArrayList<String>();
        Scanner scanner = new Scanner(docTagsOneString).useDelimiter(",\\s*");
        while (scanner.hasNext()) {
            docTags.add(scanner.next());
        }
        String createdString = ((Element) docParams.item(3)).getTextContent();
        Date created = new Date(Long.parseLong(createdString));
        String modifiedString = ((Element) docParams.item(4)).getTextContent();
        Date modified = new Date(Long.parseLong(modifiedString));
        String docDepartment = ((Element) docParams.item(5)).getTextContent();
        NodeList listChanges = ((Element) docParams.item(6)).getChildNodes();
        BasicDBObject record = new BasicDBObject();
        record.append("_id", new ObjectId(docId)).append("name", docName)
                .append("author", docAuthor).append("tags", docTags)
                .append("created", created).append("modified", modified)
                .append("department", docDepartment);
        if (currentDocument.hasAttribute("parent")) {
            ArrayList array = new ArrayList();
            record.append("draft", false);
            BasicDBObject newChange;
            NodeList currentChange;
            String user;
            String department;
            Date date;
            Boolean fileChange;
            for (int i = 0; i < listChanges.getLength(); i++) {
                currentChange = listChanges.item(i).getChildNodes();
                user = currentChange.item(0).getTextContent();
                department = currentChange.item(1).getTextContent();
                date = new Date(Long.parseLong(currentChange.item(2)
                        .getTextContent()));
                fileChange = (currentChange.item(3).getTextContent()
                        .equals("yes")) ? true : false;

                newChange = new BasicDBObject().append("name", user)
                        .append("department", department).append("date", date)
                        .append("changeFile", fileChange);
                array.add(newChange);
            }
            record.append("change", array);
            String docParentId = currentDocument.getAttributeNode("parent")
                    .getNodeValue();
            String order = ((Element) docParams.item(7)).getTextContent();
            record.append("parent", new ObjectId(docParentId)).append("order",
                    Integer.parseInt(order));
            if (((Element) docParams.item(8)).getTextContent().equals("yes")) {
                currentFile = new File((file).getAbsolutePath().replaceAll(
                        ".xml", "")
                        + "/" + docId);
                GridFSInputFile gfsFile = null;
                try {
                    gfsFile = gfsDoc.createFile(currentFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String fileName = ((Element) docParams.item(9))
                        .getTextContent();
                gfsFile.setFilename(fileName);
                gfsFile.setId(new ObjectId(docId));
                gfsFile.save();
                record.append("file", fileName);
            }
        }
        docs.save(record);
    }

    class XmlFileFilter extends FileFilter {

        @Override
        public boolean accept(File f) {
            if (f != null) {
                String name = f.getName();
                int i = name.lastIndexOf('.');

                if (i > 0 && i < name.length() - 1) {
                    return name.substring(i + 1).equals("xml");
                }

            }
            return false;
        }

        @Override
        public String getDescription() {
            return "Файл xml Docus";
        }

    }

}
package ru.restfulrobot.docus;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile;
import com.mongodb.util.JSON;

public class DocTree {

    private final Document root;
    private final DBCollection docs;

    //  главный конструктор
    public DocTree(DBCollection collection) {
        this.docs = collection;
        DBObject rootDoc = docs.findOne(new BasicDBObject("parent",
                new BasicDBObject("$exists", 0)));
        if (rootDoc == null) {
            root = new Document(null);
            root.setAuthor("...");
            root.setName("ROOT");
            root.file = false;
            root.created = new Date();
            root.modified = root.created;
            root.setDepartmentLocal("general");
            root.save();
            root.appendAncentorsInDocInformation();
        } else {
            root = new Document(null, rootDoc);
        }
    }

    // родительский документ
    public Document getMainNode() {
        return root;
    }

    public void reOrder(Document node, Document parentNode, Integer position) {
        int i = 0;
        if (parentNode == null) {

            parentNode = node.getParent();

        }

        for (Document currentDoc : parentNode.childs) {

            if (i > position) {
                currentDoc.setPosition(i);

            }
            i++;
        }
    }

    // вставить чилда MutableTreeNode
    public void insert(MutableTreeNode newChild) {

        root.insert(newChild, 0);
    }

    // добавить чилда в конец списка
    public void append(Document rootChild) {
        root.insert(rootChild, -1);
    }

    // получить всех чилдов
    public Document[] getChilds() {

        return root.getChilds();

    }

    // создание документа
    public Document createDocument(String name, String author) {
        Document doc = new Document(null);
        doc.setAuthor(author);
        doc.setName(name);
        return doc;
    }

    //  получить восходящие ветки
    public Collection<Document> getDescendens() {

        return root.getDescendens();
    }

    // получить все документы
    public Collection<Document> getAllDocuments() {
        return root.getAllDocuments();
    }

    // найти документ зная id
    public Document findByID(ObjectId string) {
        Collection<Document> nodes = root.findByID(string);
        if (nodes != null && !nodes.isEmpty()) {
            return nodes.iterator().next();
        }
        return null;
    }

    // найти документ зная имя
    public Collection<Document> findByName(String search) {
        return root.findByName(search);
    }

    // найти документы своего департмента
    public Collection<Document> findByName(String search, String department,
                                           String departments[]) {
        if (!department.equals("reviewers")) {
            departments = new String[] { department, "general" };
        }
        return root.findByName(search, departments);
    }

    // найти по автору
    public Collection<Document> findByAuthor(String search) {

        return root.findByAuthor(search);

    }

    // найти по автору своего департмента
    public Collection<Document> findByAuthor(String search, String department,
                                             String departments[]) {

        if (!department.equals("reviewers")) {
            departments = new String[] { department, "general" };
        }
        return root.findByAuthor(search, departments);
    }

    // найти по тэгам
    public Collection<Document> findTags(String search) {
        return root.findTags(search);
    }

    // найти по тэгам своего департмента
    public Collection<Document> findTags(String search, String department,
                                         String departments[]) {

        if (!department.equals("reviewers")) {
            departments = new String[] { department, "general" };
        }
        return root.findTags(search, departments);
    }

    // расширяем класс документа
    public class Document extends DefaultMutableTreeNode implements
            MutableTreeNode {
        private static final long serialVersionUID = 1L;
        public ObjectId id;
        private String name;
        private String author;
        private Date created;
        private Date modified;
        private List<String> tags = new ArrayList<String>();
        private Stack<DocusChangeDocument> changes = new Stack<DocusChangeDocument>();
        private byte[] body;
        private List<Document> childs;
        private Document parent;
        private Boolean file;
        private Boolean draft;
        private String fileName;
        private String department;
        private Integer position;
        private boolean expandable;

        //новый документ имеет родителя и арайлист чилдов
        public Document(Document parent) {
            this.parent = parent;
            id = null;
            childs = new ArrayList<Document>();
        }

        // восоздание документа и базы данных
        public Document(Document parent, DBObject object) {
            this.parent = parent;
            BasicDBObject obj = (BasicDBObject) object;
            id = obj.getObjectId("_id");
            name = obj.getString("name");
            author = obj.getString("author");
            created = obj.getDate("created");
            modified = obj.getDate("modified");
            department = obj.getString("department");
            if (parent != null) {
                position = obj.getInt("order");
            }
            file = obj.containsField("file");
            if (file) {
                fileName = obj.getString("file");
            }
            BasicDBList tagsList = (BasicDBList) obj.get("tags");
            if (tagsList != null) {
                for (Iterator<Object> it = tagsList.iterator(); it.hasNext();) {
                    tags.add(String.valueOf(it.next()));
                }
            }
            ArrayList<BasicDBObject> changesInArrayList = (ArrayList<BasicDBObject>) obj
                    .get("change");
            if (changesInArrayList != null) {
                String user, department;
                Date date;
                Boolean file;
                for (BasicDBObject change : changesInArrayList) {
                    file = change.getBoolean("changeFile");
                    date = change.getDate("date");
                    user = change.getString("name");
                    department = change.getString("department");
                    changes.push(new DocusChangeDocument(user, department,
                            file, date));
                }
            }
        }

        // предзагрузить чилды
        private void preloadChilds() {
            if (childs == null) {
                DBCursor childDocs = docs.find(new BasicDBObject("parent",
                        this.id).append("draft", false));
                childs = new ArrayList<Document>();
                List<Document> noOrderList = new ArrayList<Document>();
                while (childDocs.hasNext()) {
                    Document currentDoc = new Document(this, childDocs.next());
                    noOrderList.add(currentDoc);
                }
                int i = 0;
                int j = 0;
                while (noOrderList.size() != 0) {

                    if (j == noOrderList.get(i).getPosition()) {
                        childs.add(noOrderList.get(i));
                        noOrderList.remove(i);
                        j++;
                        i = 0;
                        continue;
                    }
                    i++;
                }

            }
        }

        // махинации с черновиками
        private void changeDrafts() {
            DBCursor draftChildDocs = docs.find(new BasicDBObject("parent",
                    this.id).append("draft", true));
            if (draftChildDocs.size() != 0) {
                BasicDBObject currentDraft;
                BasicDBObject query;
                ArrayList<ObjectId> ancestors = new ArrayList<ObjectId>();
                Document cur = this;
                while (cur != null) {
                    ancestors.add(0, cur.id);
                    cur = cur.parent;
                }
                while (draftChildDocs.hasNext()) {
                    currentDraft = (BasicDBObject) draftChildDocs.next();
                    query = new BasicDBObject("_id",
                            currentDraft.getObjectId("_id"));
                    docs.update(query, new BasicDBObject().append(
                            "$set",
                            new BasicDBObject().append("department",
                                    this.department).append("ancestors",
                                    ancestors)));
                }
            }
        }

        // достать id чилдов
        public ObjectId[] getChildsID() {
            preloadChilds();
            ObjectId[] mas = new ObjectId[childs.size()];
            int i = 0;
            for (Document child : childs) {
                mas[i++] = child.getId();
            }

            return mas;
        }

        // узнать доступ  к данному департменту
        private Boolean getAccesForThisDepartment(String department,
                                                  String[] departments) {
            for (String currentDepartment : departments) {
                if (currentDepartment.equals(department)) {
                    return true;
                }
            }
            return false;
        }

        // тэги в виде массива
        public String[] getTagsInFormatMassive() {
            int count = tags.size();
            String[] mas = new String[count];

            for (int i = 0; i < count; i++) {
                mas[i] = tags.get(i);

            }
            return mas;
        }

        // удалить сам документ и его предков
        private void delete() {
            docs.remove(new BasicDBObject("ancestors", this.id));
            docs.remove(new BasicDBObject("_id", this.id));
        }

        // коллекция документов с данным автором
        public Collection<Document> findByAuthor(String search) {
            ArrayList<Document> arrayD = new ArrayList<Document>();
            if (this.parent != null && this.author.indexOf(search) != -1)
                arrayD.add(this);
            preloadChilds();
            if (childs != null) {
                for (Document child : childs) {
                    if (child != null) {
                        arrayD.addAll(child.findByAuthor(search));
                    }
                }
            }
            return arrayD;
        }

        // коллекция документов с данным автором и департментом
        public Collection<Document> findByAuthor(String search,
                                                 String[] departments) {
            ArrayList<Document> arrayD = new ArrayList<Document>();
            if (this.parent != null && this.author.indexOf(search) != -1)
                arrayD.add(this);
            preloadChilds();
            if (childs != null) {
                for (Document child : childs) {
                    if (child != null) {
                        String departmentOfChild = child.getDepartment();
                        if (getAccesForThisDepartment(departmentOfChild,
                                departments)) {
                            arrayD.addAll(child.findByAuthor(search));
                        }
                    }
                }
            }
            return arrayD;
        }

        // проверить наличие предков
        public Boolean checkAncstors(Document doc) {
            List<ObjectId> ancestors = this.getAncestorsId();
            ObjectId docId = doc.getId();
            for (ObjectId ancestor : ancestors) {
                if (ancestor.equals(docId)) {
                    return true;
                }
            }
            return false;
        }

        // коллекция документов с данным именем
        public Collection<Document> findByName(String search) {
            ArrayList<Document> arrayD = new ArrayList<Document>();
            if (name.indexOf(search) != -1 && this.parent != null)
                arrayD.add(this);
            preloadChilds();
            if (childs != null) {
                for (Document child : childs) {
                    if (child != null) {
                        arrayD.addAll(child.findByName(search));
                    }
                }
            }
            return arrayD;
        }

        // коллекция документов с данным именем и департментом
        public Collection<Document> findByName(String search,
                                               String[] departments) {

            ArrayList<Document> arrayD = new ArrayList<Document>();
            if (this.parent != null && name.indexOf(search) != -1)
                arrayD.add(this);
            preloadChilds();
            if (childs != null) {
                for (Document child : childs) {
                    if (child != null) {
                        String departmentOfChild = child.getDepartment();
                        if (getAccesForThisDepartment(departmentOfChild,
                                departments)) {
                            arrayD.addAll(child.findByName(search));
                        }
                    }
                }
            }
            return arrayD;

        }

        // коллекция документов с данным id
        public Collection<Document> findByID(ObjectId search) {
            ArrayList<Document> arrayD = new ArrayList<Document>();
            if (id.equals(search) && parent != null)
                arrayD.add(this);
            preloadChilds();
            if (childs != null) {
                for (Document child : childs) {
                    if (child != null) {
                        arrayD.addAll(child.findByID(search));
                    }
                }
            }
            return arrayD;
        }

        // получить id предков
        public List<ObjectId> getAncestorsId() {
            ArrayList<ObjectId> ancestors = new ArrayList<ObjectId>();

            Document cur = this.parent;
            while (cur != null) {
                ancestors.add(0, cur.id);
                cur = cur.parent;
            }
            return ancestors;
        }

        // получить коллекцию предков
        public List<Document> getAncestors() {
            ArrayList<Document> ancestors = new ArrayList<Document>();

            Document cur = this.parent;
            while (cur != root) {
                ancestors.add(0, cur);
                cur = cur.parent;
            }
            return ancestors;
        }

        // получить все документы
        public List<Document> getAllDocuments() {
            ArrayList<Document> arrayD = new ArrayList<Document>();
            arrayD.add(this);
            preloadChilds();
            if (childs != null) {
                for (Document child : childs) {
                    if (child != null) {
                        arrayD.addAll(child.getAllDocuments());
                    }
                }
            }
            return arrayD;
        }

        // имя документа является его предсталвением в дереве
        @Override
        public String toString() {
            return this.name;
        }

        private boolean compareTags(String search) {
            for (String string : tags) {
                if (search.equals(string)) {
                    return true;
                }
            }
            return false;
        }

        // коллекция документов с данными тэгами
        public Collection<Document> findTags(String search) {
            ArrayList<Document> arrayD = new ArrayList<Document>();
            if (this.compareTags(search)) {
                arrayD.add(this);
            }
            preloadChilds();
            if (childs != null) {
                for (Document child : childs) {
                    if (child != null) {
                        arrayD.addAll(child.findTags(search));
                    }
                }
            }
            return arrayD;
        }

        // коллекция документов с данными тэгами и департментом
        public Collection<Document> findTags(String search, String[] departments) {
            ArrayList<Document> arrayD = new ArrayList<Document>();
            if (this.compareTags(search)) {
                arrayD.add(this);
            }
            preloadChilds();
            if (childs != null) {
                for (Document child : childs) {
                    if (child != null) {
                        String departmentOfChild = child.getDepartment();
                        if (getAccesForThisDepartment(departmentOfChild,
                                departments)) {
                            arrayD.addAll(child.findTags(search));
                        }
                    }
                }
            }
            return arrayD;
        }

        // получить всех чилдов от чилдов (потомки)
        public Collection<Document> getDescendens() {

            ArrayList<Document> arrayD = new ArrayList<Document>();
            arrayD.add(this);
            if (childs != null) {
                for (Document child : childs) {
                    if (child != null) {
                        arrayD.addAll(child.getDescendens());
                    }
                }
            }
            return arrayD;
        }

        public ObjectId getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public Date getCreated() {
            return created;
        }

        public Date getModified() {
            return modified;
        }

        public void setModified(Date modified) {
            this.modified = modified;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public byte[] getBody() {
            return body;
        }

        public void setBody(byte[] body) {
            this.body = body;
        }

        // получить чилдов в виде массива
        public Document[] getChilds() {
            preloadChilds();
            return childs.toArray(new Document[childs.size()]);
        }

        // получить предка
        public Document getParent() {
            return parent;
        }

        // получить чилда по id
        @Override
        public TreeNode getChildAt(int childIndex) {
            preloadChilds();
            return (TreeNode) childs.get(childIndex);
        }

        // получить кол-во чилдов
        @Override
        public int getChildCount() {
            preloadChilds();
            return childs.size();
        }

        // получить индекс текущего чилда
        @Override
        public int getIndex(TreeNode node) {
            preloadChilds();
            return childs.indexOf(node);
        }

        @Override
        public boolean getAllowsChildren() {
            return true;
        }

        // нет потомков?
        @Override
        public boolean isLeaf() {
            preloadChilds();
            return childs.isEmpty();
        }

        // Енумератор для чилдов
        @Override
        public Enumeration<TreeNode> children() {
            if (childs == null) {
                return Collections.enumeration(Collections.EMPTY_LIST);
            } else {
                final Iterator<Document> it = childs.iterator();
                Enumeration<TreeNode> enumer = new Enumeration<TreeNode>() {
                    public boolean hasMoreElements() {
                       return it.hasNext();
                    }
                    public TreeNode nextElement() {
                        return it.next();
                    }
                };
                return enumer;
            }
        }

        // вставить нового потомка в этот  документ
        @Override
        public void insert(MutableTreeNode newChild, int index) {
            preloadChilds();
            MutableTreeNode oldParent = (MutableTreeNode) newChild.getParent();
            if (oldParent != null) {
                oldParent.remove(newChild);
            }
            newChild.setParent(this);
            if (index == -1) {
                childs.add((Document) newChild);
            } else {
                childs.add(index, (Document) newChild);
            }
            for (Document childOfNewChild : ((Document) newChild).getChilds()) {
                childOfNewChild.returnChilds();
            }
            ((Document) newChild).changeDrafts();

        }

        // вставка чилда в общем родителе "до"
        public void insertBeforeInGeneralParent(MutableTreeNode child,
                                                int positionDestinationDoc, int positionSourceDoc) {

            preloadChilds();
            childs.remove(child);
            if (positionDestinationDoc > positionSourceDoc) {

                childs.add(positionDestinationDoc - 1, (Document) child);

            } else {
                childs.add(positionDestinationDoc, (Document) child);

            }

            int i = 0;
            for (Document currentDoc : childs) {
                currentDoc.setPosition(i);
                i++;
            }
        }

        // вставка чилда в общем родителе "после"
        public void insertAfterInGeneralParent(MutableTreeNode child,
                                               int positionDestinationDoc, int positionSourceDoc) {

            preloadChilds();
            childs.remove(child);
            if (positionDestinationDoc > positionSourceDoc) {
                if ((positionDestinationDoc + 1) == childs.size()) {
                    childs.add((Document) child);
                } else {
                    childs.add(positionDestinationDoc, (Document) child);
                }
            } else {
                childs.add(positionDestinationDoc + 1, (Document) child);

            }
            int i = 0;
            for (Document currentDoc : childs) {
                currentDoc.setPosition(i);
                i++;
            }
        }

        // удаление
        @Override
        public void remove(int index) {
            MutableTreeNode child = (MutableTreeNode) getChildAt(index);
            childs.remove(index);
            child.setParent(null);
        }

        // чилды чилдов в дб коллектион
        public void returnChilds() {
            BasicDBObject record = new BasicDBObject().append("_id", id)
                    .append("name", name).append("author", author)
                    .append("created", getCreated())
                    .append("modified", getModified()).append("tags", tags);
            position = this.getPosition();
            record.append("parent", parent.id)
                    .append("ancestors", getAncestorsId())
                    .append("order", position).append("draft", false);
            docs.save(record);
            preloadChilds();
            for (Document doc : childs) {
                doc.returnChilds();
            }

        }

        // информация о предках
        public void appendAncentorsInDocInformation() {
            BasicDBObject thisNode = (BasicDBObject) docs
                    .findOne(new BasicDBObject("_id", id));
            thisNode.append("ancestors", getAncestorsId());
            docs.save(thisNode);
        }

        // удаление
        @Override
        public void remove(MutableTreeNode node) {
            remove(getIndex(node));
        }

        @Override
        public void setUserObject(Object object) {
        }

        // удаление из парнета
        @Override
        public void removeFromParent() {
            MutableTreeNode parent = (MutableTreeNode) getParent();
            if (parent != null) {
                parent.remove(this);
            }

        }

        // установить парнета
        @Override
        public void setParent(MutableTreeNode newParent) {
            if (newParent != parent) {
                parent = (Document) newParent;
                if (newParent == null) {
                    delete();
                } else {
                    save();
                }
            }
        }

        // сохрнаить новый док
        public void save() {
            if (id == null) {
                id = new ObjectId();
                created = new Date();
            }
            setModified(new Date());
            BasicDBObject record = new BasicDBObject().append("_id", id)
                    .append("name", name).append("author", author)
                    .append("created", getCreated())
                    .append("modified", getModified()).append("tags", tags)
                    .append("department", department);
            if (this != root) {
                position = this.getParent().getChildCount();
                // throws NullPointerException if document has no parent and is
                // not root
                record.append("parent", parent.id)
                        .append("ancestors", getAncestorsId())
                        .append("order", position);
            }
            docs.save(record);
        }

        // первое измененеи объекта
        public void firstChange(String name, String department) {
            BasicDBObject queryDB = new BasicDBObject();
            BasicDBObject changeFieldDB = new BasicDBObject();
            queryDB.put("_id", this.getId());
            changeFieldDB.append("$set", new BasicDBObject("change",
                    new ArrayList()));
            docs.update(queryDB, changeFieldDB);
            changes.push(new DocusChangeDocument(name, department, false,
                    created));
            changeFieldDB = new BasicDBObject();
            BasicDBObject newChange = new BasicDBObject()
                    .append("date",
                            ((DocusChangeDocument) changes.peek()).getDate())
                    .append("name",
                            ((DocusChangeDocument) changes.peek()).getName())
                    .append("department",
                            ((DocusChangeDocument) changes.peek())
                                    .getDepartment())
                    .append("changeFile",
                            ((DocusChangeDocument) changes.peek())
                                    .getChangeFile());
            changeFieldDB.append("$push",
                    new BasicDBObject("change", newChange));
            docs.update(queryDB, changeFieldDB);
        }

        // обновленеи объекта
        public void update() {
            BasicDBObject queryDB = new BasicDBObject();
            BasicDBObject changeFieldDB = new BasicDBObject();
            queryDB.put("_id", this.getId());
            BasicDBObject newChange = new BasicDBObject()
                    .append("date",
                            ((DocusChangeDocument) changes.peek()).getDate())
                    .append("name",
                            ((DocusChangeDocument) changes.peek()).getName())
                    .append("department",
                            ((DocusChangeDocument) changes.peek())
                                    .getDepartment())
                    .append("changeFile",
                            ((DocusChangeDocument) changes.peek())
                                    .getChangeFile());

            changeFieldDB.append(
                    "$set",
                    new BasicDBObject().append("author", author)
                            .append("name", name).append("tags", tags)
                            .append("modified", modified)).append("$push",
                    new BasicDBObject("change", newChange));
            docs.update(queryDB, changeFieldDB);

        }

        // есть ли файл?
        public Boolean getFile() {
            return file;

        }

        // установка файла
        public void setFile(Boolean file, String name) {
            fileName = name;
            this.file = file;
            modified = new Date();
            DBObject queryDB = new BasicDBObject();
            queryDB.put("_id", id);
            BasicDBObject changeFieldDB = new BasicDBObject().append(
                    "$set",
                    new BasicDBObject().append("file", name).append("modified",
                            modified));
            docs.update(queryDB, changeFieldDB);
        }

        // удаление файла
        public void setFile(Boolean file) {
            this.file = file;
            BasicDBObject queryDB = new BasicDBObject();
            queryDB.put("_id", this.getId());
            docs.update(queryDB,
                    (DBObject) JSON.parse("{$unset : { file: \"\" }}"));

        }

        // сделать черновиком
        public void setDraft(Boolean draft) {
            this.draft = draft;
            BasicDBObject queryDB = new BasicDBObject();
            queryDB.put("_id", this.getId());
            docs.update(queryDB, new BasicDBObject().append("$set",
                    new BasicDBObject("draft", draft)));
        }

        // черновик?
        public Boolean getDraft() {
            return draft;
        }

        // установить позицию
        public void setPosition(Integer position) {
            this.position = position;
            DBObject queryDB = new BasicDBObject();
            queryDB.put("_id", this.getId());
            BasicDBObject changeFieldDB = new BasicDBObject().append("$set",
                    new BasicDBObject().append("order", position));
            docs.update(queryDB, changeFieldDB);

        }

        // установить департмент
        public void setDepartmentLocal(String department) {
            this.department = department;
            DBObject queryDB = new BasicDBObject();
            queryDB.put("_id", this.getId());
            BasicDBObject changeFieldDB = new BasicDBObject().append("$set",
                    new BasicDBObject().append("department", department));
            docs.update(queryDB, changeFieldDB);
        }

        // получить департмент
        public String getDepartment() {
            return department;
        }

        public void setDepartmentGlobal(String department) {
            setDepartmentLocal(department);
            preloadChilds();
            if (childs != null) {
                for (Document child : childs) {
                    if (child != null) {
                        child.setDepartmentGlobal(department);

                    }
                }
            }
        }

        // достать позицию
        public Integer getPosition() {
            return position;
        }

        // узнать имя файла
        public String getFileName() {
            return fileName;
        }

        //  загрузка файлов
        public void setFile(String filename, byte[] filecontent) {
            GridFS gfsDoc = DocusRegistry.instance.getGridFS("DocFiles");
            GridFSInputFile gfsFile = null;
            gfsFile = gfsDoc.createFile(filecontent);
            gfsFile.setFilename(fileName);
            gfsFile.setId(id);
            gfsFile.save();
            setFile(true, fileName);
        }

        // установить расширяемость
        public void setExpandable(boolean expandable) {
            this.expandable = expandable;
        }

        // расширяемый?
        public boolean isExpandable() {
            return expandable;
        }

        // принятие изменений
        public void appendChange(String userName, String department,
                                 Boolean changeFile, Date change) {
            changes.push(new DocusChangeDocument(userName, department,
                    changeFile, change));
        }

        // стэк изменений
        public Stack<DocusChangeDocument> getChanges() {
            return changes;
        }

    }

}

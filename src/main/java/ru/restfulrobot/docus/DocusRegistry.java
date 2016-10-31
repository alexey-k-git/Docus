package ru.restfulrobot.docus;

import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.gridfs.GridFS;

public class DocusRegistry {

    public static final DocusRegistry instance = new DocusRegistry();
    private MongoClient mongo;
    private DB db;
    private DBCollection collectionDoc;
    private DBCollection collectionUser;
    private GridFSMap gridFS = new GridFSMap();

    private DocTree docTree;

    private DocusRegistry() {
        super();
    }

    public void initDbConnection() throws UnknownHostException {
        if (mongo == null) {
            mongo = new MongoClient("localhost", 27017);
            db = mongo.getDB("documents");
            collectionUser = db.getCollection("users");
            collectionDoc = db.getCollection("docs");
        }
    }

    public void initDocTree() {
        if (docTree == null) {
            docTree = new DocTree(collectionDoc);
        }
    }

    public DBCollection getCollectionDoc() {
        return collectionDoc;
    }

    public DocTree getDocTree() {
        return docTree;
    }

    public DBCollection getCollectionUser() {
        return collectionUser;
    }

    public GridFS getGridFS(String name) {
        GridFS fs = gridFS.get(name);
        if (fs == null) {
            fs = new GridFS(db, name);
            gridFS.put(name, fs);
        }
        return fs;
    }

    private class GridFSMap extends LinkedHashMap<String, GridFS>{
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Entry<String, GridFS> eldest) {
            return super.size() > 2;
        }

    }
}
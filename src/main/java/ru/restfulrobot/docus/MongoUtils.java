package ru.restfulrobot.docus;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class MongoUtils {

    private MongoUtils() {
        super();
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
}
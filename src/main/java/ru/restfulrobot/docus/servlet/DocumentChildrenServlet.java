package ru.restfulrobot.docus.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.restfulrobot.docus.DocusRegistry;
import ru.restfulrobot.docus.MongoUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.util.JSON;

public class DocumentChildrenServlet extends JsonServlet {

    /** Unique Id */
    private static final long serialVersionUID = 5526714418328894690L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String nodeId = request.getParameter("id");
        final BasicDBObject query = new BasicDBObject();
        if (nodeId == null || "root".equals(nodeId)) {
            query.append("parent", new BasicDBObject("$exists", 0));
        } else {
            query.append("parent", new ObjectId(nodeId));
        }
        DBCursor childs = DocusRegistry.instance.getCollectionDoc().find(query).sort(
                new BasicDBObject("order", 1));
        try {
            JSONArray array = new JSONArray();
            while (childs.hasNext()) {
                JSONObject object = new JSONObject(JSON.serialize(MongoUtils.toDocumentNode(childs.next())));
                array.put(object);
            }
            writeJson(array, response);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

}

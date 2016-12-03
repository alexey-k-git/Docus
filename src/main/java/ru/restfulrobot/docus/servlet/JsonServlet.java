package ru.restfulrobot.docus.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

abstract public class JsonServlet extends HttpServlet {

    private static final long serialVersionUID = 7062751078308112944L;

    protected void setJsonHeaders(HttpServletResponse resp) {
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Cache-Control", "must-revalidate, no-cache, no-store");
        resp.setDateHeader("Expires", 0);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("utf-8");
    }

    protected void writeJson(JSONObject object, HttpServletResponse resp) throws IOException {
        setJsonHeaders(resp);
        PrintWriter writer = resp.getWriter();
        writer.println(object.toString());
    }

    protected void writeJson(JSONArray object, HttpServletResponse resp) throws IOException {
        setJsonHeaders(resp);
        PrintWriter writer = resp.getWriter();
        writer.println(object.toString());
    }
}

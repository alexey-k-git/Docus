package ru.restfulrobot.docus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


import org.bson.types.ObjectId;


import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

public class DocusComments extends JFrame {

    private JTextArea newCommentText;
    private MongoClient mongo;
    private DB db;
    private DBCollection comments;
    private DocusFrame mainFrame;
    private String department;
    private String user;
    private ObjectId id;
    private JPanel commentsPanel;
    private JScrollPane spCommentPanel;
    private JPanel mainPanel;
    private JButton buttonAddNewComment;
    private JFrame self;
    private Boolean firstComment;

    public DocusComments(MongoClient mongoClient, DocusFrame mainFrame, String user,
                         String department, ObjectId id) {
        super();
        initConnection(mongoClient);
        init(mainFrame, user, department,id);
    }

    public void init(DocusFrame docusFrame, String user, String department, ObjectId id) {
        this.department = department;
        this.user = user;
        this.mainFrame = docusFrame;
        this.mainFrame.setEnabled(false);
        this.id=id;
        self=this;
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mainFrame.setEnabled(true);
                mainFrame.removeCommentsForm();
            }
        });
        newCommentText = new JTextArea(7,30);
        newCommentText.setLineWrap(true);
        newCommentText.setWrapStyleWord(true);
        newCommentText.setBorder(BorderFactory.createLineBorder(Color.black));
        JScrollPane spComment = new JScrollPane(newCommentText);
        buttonAddNewComment = new JButton(new ButtonAddNewCommentListener());
        mainPanel = new JPanel(new BorderLayout(5,5));
        mainPanel.setBackground(Color.white);
        JPanel newCommentPanel = new JPanel(new BorderLayout());
        newCommentPanel.add(spComment, BorderLayout.CENTER);
        newCommentPanel.add(buttonAddNewComment, BorderLayout.SOUTH);
        commentsPanel = new JPanel();
        spCommentPanel = new JScrollPane(commentsPanel);
        mainPanel.add(newCommentPanel, BorderLayout.SOUTH);
        mainPanel.add(spCommentPanel, BorderLayout.CENTER);
        updateComments();
        setTitle("Просмотр коментариев к документу");
        setContentPane(mainPanel);
        setSize(600, 600);
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);

    }

    private void initConnection(MongoClient mongoClient) {
        mongo = mongoClient;
        db = mongo.getDB("documents");
        comments = db.getCollection("comments");
    }

    private ArrayList<BasicDBObject> getComments() {
        DBCursor cursor = comments.find(new BasicDBObject("_id",
                id));
        if (cursor.count()==0)
        {
            return null;
        }
        else
        {
            BasicDBObject user = (BasicDBObject) cursor.next();
            return  (ArrayList<BasicDBObject>)user.get("comments");
        }
    }




    private void updateComments() {
        mainPanel.remove(spCommentPanel);
        ArrayList<BasicDBObject> comments = getComments();
        firstComment = false;
        if (comments==null)
        {
            commentsPanel = new JPanel();
            commentsPanel.add(new JLabel("Не было добавлено ещё ни одного комментария..."));
            firstComment = true;
        }
        else
        {
            commentsPanel = new JPanel(new GridLayout(0,1));;
            JPanel userInfoPanel;
            String user, department, text;
            JTextArea userComment;
            Date date;
            for (BasicDBObject comment: comments)
            {
                JPanel info = new JPanel();
                date = comment.getDate("date");
                user = comment.getString("user");
                department = comment.getString("department");
                text = comment.getString("text");
                userInfoPanel = new JPanel(new GridLayout(0,1));
                userInfoPanel.add(new JLabel("Пользователь:"));
                userInfoPanel.add(new JLabel(user));
                userInfoPanel.add(new JLabel("Подразделение:"));
                userInfoPanel.add(new JLabel(department));
                userInfoPanel.add(new JLabel("Число:"));
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                String mydateStr = df.format(date);
                userInfoPanel.add(new JLabel(mydateStr));
                userInfoPanel.add(new JLabel("Время:"));
                df = new SimpleDateFormat("HH:mm:ss aa");
                mydateStr = df.format(date);
                userInfoPanel.add(new JLabel(mydateStr));
                userInfoPanel.setPreferredSize(new Dimension(200,120));
                userComment = new JTextArea(7,30);
                userComment.setText(text);
                userComment.setEditable(false);
                userComment.setBorder(BorderFactory.createLineBorder(Color.gray));
                userComment.setLineWrap(true);
                userComment.setWrapStyleWord(true);
                info.add(userInfoPanel);
                info.add(userComment);
                commentsPanel.add(info);
            }
        }
        spCommentPanel = new JScrollPane(commentsPanel);
        spCommentPanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mainPanel.add(spCommentPanel, BorderLayout.CENTER);
        mainPanel.revalidate();
    }


    private void addComment(String text) {
        if (firstComment)
        {
            BasicDBObject commentsforDoc = new BasicDBObject("_id", id);
            commentsforDoc.append("comments", new ArrayList());
            comments.insert(commentsforDoc);
        }
        BasicDBObject newComment = new BasicDBObject()
                .append("user", user)
                .append("department", department)
                .append("date",new Date())
                .append("text",	text);
        BasicDBObject queryDB = new BasicDBObject();
        queryDB.put("_id", id);
        BasicDBObject changeFieldDB = new BasicDBObject();
        changeFieldDB.append("$push",
                new BasicDBObject("comments", newComment));
        comments.update(queryDB, changeFieldDB);
        newCommentText.setText("");
        Object[] options = { "Окей" };
        JOptionPane.showOptionDialog(self, "Ваш коментарий успешно добавлен.",
                "Информация!", JOptionPane.OK_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
    }


    protected class ButtonAddNewCommentListener extends AbstractAction {

        public ButtonAddNewCommentListener() {
            putValue(Action.NAME, "Добавить");
            putValue(Action.SHORT_DESCRIPTION,
                    "Добавить свой комментарий к документу");
        }

        public void actionPerformed(ActionEvent arg0) {

            String text = newCommentText.getText();
            if (text.length()<10)
            {

                Object[] options = { "Окей" };
                JOptionPane.showOptionDialog(self, "Недостаточно емкий текст комментария.\nДанный комментарий не будет добавлен.",
                        "Информация!", JOptionPane.OK_OPTION,
                        JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
            }
            else
            {
                addComment(text);
                updateComments();
            }



        }

    }

}


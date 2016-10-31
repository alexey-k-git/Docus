package ru.restfulrobot.docus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

public class DocusAuthorizationForm extends JFrame

{
    private JTextField textLogin;
    private JTextField textPassword;
    private MongoClient mongo;
    private DB db;
    private DBCollection users;
    private DocusFrame mainFrame;

    public DocusAuthorizationForm(MongoClient mongoClient, DocusFrame mainFrame) {
        super();
        initConnection(mongoClient);
        init(mainFrame);
    }

    public void init(DocusFrame docusFrame) {
        this.mainFrame = docusFrame;
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mainFrame.removeLoginForm();
            }
        });
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.white);
        setTitle("Форма Авторизации");
        this.setContentPane(mainPanel);
        JButton enter = new JButton(new ButtonEnterListener());
        JLabel label = new JLabel("Ваши данные:");
        label.setHorizontalAlignment(0);
        JLabel labelLogin = new JLabel("Логин:");
        JLabel labelPassword = new JLabel("Пароль:");
        JPanel form = new JPanel(new FlowLayout(FlowLayout.CENTER, 999, 5));
        form.setPreferredSize(new Dimension(300, 400));
        textLogin = new JTextField(20);
        textLogin.setBorder(BorderFactory.createLineBorder(Color.black));
        textPassword = new JTextField(20);
        textPassword.setBorder(BorderFactory.createLineBorder(Color.black));
        form.add(label);
        form.add(labelLogin);
        form.add(textLogin);
        form.add(labelPassword);
        form.add(textPassword);
        form.add(enter);
        mainPanel.add(form, BorderLayout.CENTER);
        mainPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        this.setSize(350, 180);
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setVisible(true);

    }

    private Boolean checkEmptyFields() {
        Object[] options = { "Окей" };
        if (textLogin.getText().equals("") & textPassword.getText().equals("")) {
            JOptionPane
                    .showOptionDialog(
                            this,
                            "Поля Логина и Пароля не заполнены.\n Введите,пожалуйста, свои данные.",
                            "Ошибка", JOptionPane.OK_OPTION,
                            JOptionPane.ERROR_MESSAGE, null, options,
                            options[0]);
            return false;
        } else if (textLogin.getText().equals("")) {

            JOptionPane
                    .showOptionDialog(
                            this,
                            "Поле Логина не заполнено.\n Введите,пожалуйста, свой логин.",
                            "Ошибка", JOptionPane.OK_OPTION,
                            JOptionPane.ERROR_MESSAGE, null, options,
                            options[0]);

            return false;
        } else if (textPassword.getText().equals("")) {

            JOptionPane
                    .showOptionDialog(
                            this,
                            "Поле Пароля не заполнено.\n Введите,пожалуйста, свои пароль.",
                            "Ошибка", JOptionPane.OK_OPTION,
                            JOptionPane.ERROR_MESSAGE, null, options,
                            options[0]);

            return false;

        } else {
            return true;
        }
    }

    private void authorization() {
        Object[] options = { "Окей" };
        BasicDBObject user = (BasicDBObject) users.findOne(new BasicDBObject(
                "_id", textLogin.getText()));
        if (user == null) {
            JOptionPane
                    .showOptionDialog(
                            this,
                            "Польователь с таким именем не найден.\n Проверьте правильность ввода.",
                            "Ошибка", JOptionPane.OK_OPTION,
                            JOptionPane.ERROR_MESSAGE, null, options,
                            options[0]);
        } else {
            if (user.getString("password").equals(
                    DocusApplication.encodePassword(textPassword.getText(),
                            "DocusForever"))) {
                String name = user.getString("_id");
                String role = "";
                String department = user.getString("department");
                Boolean isChief = user.containsKey("chief");
                BasicDBList roleList = (BasicDBList) user.get("roles");
                if (department.equals("reviewers")) {
                    BasicDBList departmentList = (BasicDBList) user
                            .get("controls");
                    String departments[] = new String[departmentList.size()];
                    int i = 0;
                    for (Iterator<Object> it = departmentList.iterator(); it
                            .hasNext();) {
                        departments[i] = (String.valueOf(it.next()));
                        i++;
                    }
                    mainFrame.authorization(role, name, department, isChief,
                            departments);

                } else {
                    mainFrame.authorization(role, name, department, isChief,
                            null);
                }
                mainFrame.removeLoginForm();
                this.dispose();
            }

            else {
                JOptionPane
                        .showOptionDialog(
                                this,
                                "Данный пароль не соответствует введенному логину.\n Проверьте правильность ввода.",
                                "Ошибка", JOptionPane.OK_OPTION,
                                JOptionPane.ERROR_MESSAGE, null, options,
                                options[0]);

            }

        }

    }

    private void initConnection(MongoClient mongoClient) {
        mongo = mongoClient;
        db = mongo.getDB("documents");
        users = db.getCollection("users");
    }

    protected class ButtonEnterListener extends AbstractAction {

        public ButtonEnterListener() {
            putValue(Action.NAME, "Вход");
            putValue(Action.SHORT_DESCRIPTION, "Авторизоваться");
        }

        public void actionPerformed(ActionEvent arg0) {

            if (checkEmptyFields()) {
                authorization();
            }

        }

    }

}
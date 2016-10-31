package ru.restfulrobot.docus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

public class DocusRegistrationForm extends JFrame {
    private JTextField textLogin;
    private JPasswordField textPassword;
    private JPasswordField textConfirmPassword;
    private MongoClient mongo;
    private DB db;
    private DBCollection users;
    private DocusFrame mainFrame;

    public DocusRegistrationForm(MongoClient mongoClient, DocusFrame mainFrame) {
        super();
        initConnection(mongoClient);
        init(mainFrame);
    }

    public void init(DocusFrame docusFrame) {
        this.mainFrame = docusFrame;
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mainFrame.removeRegistrationForm();
            }
        });
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.white);
        setTitle("Форма Регистрации");
        this.setContentPane(mainPanel);
        JButton reg = new JButton(new ButtonRegListener());
        JLabel label = new JLabel("Данные для нового пользователя:");
        label.setHorizontalAlignment(0);
        JLabel labelLogin = new JLabel("Логин:");
        JLabel labelPassword = new JLabel("Пароль:");
        JLabel labelConfirmPassword = new JLabel("Подтверждение пароля:");
        JPanel form = new JPanel(new FlowLayout(FlowLayout.CENTER, 999, 8));
        form.setPreferredSize(new Dimension(350, 260));
        textLogin = new JTextField(20);
        textLogin.setBorder(BorderFactory.createLineBorder(Color.black));
        textPassword = new JPasswordField(20);
        textPassword.setBorder(BorderFactory.createLineBorder(Color.black));
        textConfirmPassword = new JPasswordField(20);
        textConfirmPassword.setBorder(BorderFactory
                .createLineBorder(Color.black));
        form.add(label);
        form.add(labelLogin);
        form.add(textLogin);
        form.add(labelPassword);
        form.add(textPassword);
        form.add(labelConfirmPassword);
        form.add(textConfirmPassword);
        form.add(reg);
        mainPanel.add(form, BorderLayout.CENTER);
        mainPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        this.setSize(350, 260);
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setVisible(true);

    }

    private Boolean checkFields() {
        Object[] options = { "Окей" };
        if (textLogin.getText().equals("")
                || textPassword.getPassword().length == 0
                || textConfirmPassword.getPassword().length == 0) {
            JOptionPane
                    .showOptionDialog(
                            this,
                            "Одно или несколько полей пустые.\n Заполните,пожалуйста, их данными.",
                            "Ошибка", JOptionPane.OK_OPTION,
                            JOptionPane.ERROR_MESSAGE, null, options,
                            options[0]);
            return false;
        } else if (! Arrays.equals(textPassword.getPassword(),
                textConfirmPassword.getPassword())) {

            JOptionPane.showOptionDialog(this,
                    "Пароли не совпадают.\n Попробуйте ввести снова.",
                    "Ошибка", JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE,
                    null, options, options[0]);

            return false;
        } else if (checkUserName(textLogin.getText())) {
            JOptionPane
                    .showOptionDialog(
                            this,
                            "Данное имя уже существует в базе.\n Придумайте, пожалуйста, другое.",
                            "Ошибка", JOptionPane.OK_OPTION,
                            JOptionPane.INFORMATION_MESSAGE, null, options,
                            options[0]);

            return false;

        } else {
            return true;
        }
    }

    private boolean checkUserName(String name) {
        BasicDBObject user = (BasicDBObject) users.findOne(new BasicDBObject(
                "_id", name));
        if (user == null) {
            return false;
        }
        return true;
    }

    private void registration() {
        DBObject newUser;
        String name = textLogin.getText();
        String password = new String(textPassword.getPassword());
        newUser = new BasicDBObject("_id", name)
                .append("password",
                        DocusApplication.encodePassword(password,
                                "DocusForever"))
                .append("department", "general");
        users.insert(newUser);
        Object[] options = { "Окей" };
        JOptionPane.showOptionDialog(this,
                "Был создан новый пользователь с именем \"" + name + "\"",
                "Успех!", JOptionPane.OK_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
        String role = "visitor";
        mainFrame.authorization(role, name, "general", false, null);
        mainFrame.removeRegistrationForm();
        this.dispose();
    }

    private void initConnection(MongoClient mongoClient) {
        mongo = mongoClient;
        db = mongo.getDB("documents");
        users = db.getCollection("users");
    }

    protected class ButtonRegListener extends AbstractAction {

        public ButtonRegListener() {
            putValue(Action.NAME, "Зарегистрироваться");
            putValue(Action.SHORT_DESCRIPTION,
                    "Зарегистрировать нового пользователя");
        }

        public void actionPerformed(ActionEvent arg0) {

            if (checkFields()) {
                registration();
            }

        }

    }

}
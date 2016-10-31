package ru.restfulrobot.docus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ru.restfulrobot.docus.DocTree.Document;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

public class DocusAppointDepartmentForm extends JFrame {

    private MongoClient mongo;
    private DB db;
    private DBCollection departments;
    private DocusFrame mainFrame;
    private String department;
    private Document document;
    private JLabel labelValueCurrentDepartment;

    public DocusAppointDepartmentForm(MongoClient mongoClient,
                                      DocusFrame mainFrame, Document document) {
        super();
        initConnection(mongoClient);
        init(mainFrame, document);
        mainFrame.setEnabled(false);
    }

    public void init(DocusFrame docusFrame, Document document) {
        this.mainFrame = docusFrame;
        this.document = document;
        String[] items = getDepartments();
        department = items[0];
        this.addWindowListener(new AppointDepartmentWindowAdapter());
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.white);
        setTitle("Назначения подразделение");
        this.setContentPane(mainPanel);
        JButton appoint = new JButton(new ButtonAppointListener());
        JButton disappoint = new JButton(new ButtonDisappointListener());
        JLabel labelCurrentDepartment = new JLabel("Текущее подразделение:");
        labelValueCurrentDepartment = new JLabel();
        JLabel labelChooseDepartment = new JLabel("Выбор подразделения:");
        labelChooseDepartment.setHorizontalAlignment(0);
        labelValueCurrentDepartment.setHorizontalAlignment(0);
        labelCurrentDepartment.setHorizontalAlignment(0);
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JComboBox<?> box = (JComboBox<?>) e.getSource();
                department = (String) box.getSelectedItem();
            }
        };
        JComboBox<?> comboBox = new JComboBox<Object>(items);
        comboBox.setAlignmentX(LEFT_ALIGNMENT);
        comboBox.addActionListener(actionListener);
        JPanel form = new JPanel(new FlowLayout(FlowLayout.CENTER, 999, 5));
        form.setPreferredSize(new Dimension(350, 250));
        form.add(labelCurrentDepartment);
        form.add(labelValueCurrentDepartment);
        form.add(labelChooseDepartment);
        form.add(comboBox);
        form.add(appoint);
        form.add(disappoint);
        updateDepartmentlabel();
        mainPanel.add(form, BorderLayout.CENTER);
        mainPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        this.setSize(350, 250);
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    private String[] getDepartments() {
        String departmentArray[] = new String[safeLongToInt(departments.count())];
        DBCursor cursor = departments.find();
        int i = 0;
        DBObject department;
        while (cursor.hasNext()) {
            department = cursor.next();
            departmentArray[i] = department.get("_id").toString();
            i++;
        }
        return departmentArray;
    }

    private int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(l
                    + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    private void initConnection(MongoClient mongoClient) {
        mongo = mongoClient;
        db = mongo.getDB("documents");
        departments = db.getCollection("departments");
    }

    private void updateDepartmentlabel() {
        labelValueCurrentDepartment.setText(document.getDepartment());
    }

    protected class ButtonAppointListener extends AbstractAction {

        public ButtonAppointListener() {
            putValue(Action.NAME, "Назначить");
            putValue(Action.SHORT_DESCRIPTION,
                    "Сделать папку принадлежащей данному подразделению");
        }

        public void actionPerformed(ActionEvent arg0) {
            document.setDepartmentGlobal(department);
            updateDepartmentlabel();
        }

    }

    protected class ButtonDisappointListener extends AbstractAction {

        public ButtonDisappointListener() {
            putValue(Action.NAME, "Сбросить");
            putValue(Action.SHORT_DESCRIPTION,
                    "Сбросить принадлежность данной папки к подразделению");
        }

        public void actionPerformed(ActionEvent arg0) {
            document.setDepartmentGlobal("general");
            updateDepartmentlabel();
        }

    }


    public class AppointDepartmentWindowAdapter implements WindowListener {

        @Override
        public void windowClosing(WindowEvent e) {
            mainFrame.setEnabled(true);
            mainFrame.removeAppointDepartmentForm(document);
        }

        @Override
        public void windowActivated(WindowEvent e) {
        }

        @Override
        public void windowClosed(WindowEvent e) {
        }

        @Override
        public void windowDeactivated(WindowEvent e) {
        }

        @Override
        public void windowDeiconified(WindowEvent e) {
        }

        @Override
        public void windowIconified(WindowEvent e) {
        }

        @Override
        public void windowOpened(WindowEvent e) {
        }

    }
}

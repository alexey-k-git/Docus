package ru.restfulrobot.docus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

public class DocusControlDepartmentForm extends JFrame {

    private JTextField textNameDeparment;
    private MongoClient mongo;
    private DB db;
    private DBCollection departments;
    private DBCollection users;
    private DBCollection docs;
    private DocusFrame mainFrame;
    private JTable tableDepartments;

    public DocusControlDepartmentForm(MongoClient mongoClient,
                                      DocusFrame mainFrame) {
        super();
        initConnection(mongoClient);
        init(mainFrame);
    }

    public void init(DocusFrame docusFrame) {
        this.mainFrame = docusFrame;
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mainFrame.removeControlDepartmentForm();
            }
        });
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.white);
        setTitle("Форма управления подразделениями");
        this.setContentPane(mainPanel);
        JLabel labelNewDepartment = new JLabel("Имя нового отдела:");
        JLabel labelDepartments = new JLabel("Таблица с отделами");
        labelNewDepartment.setHorizontalAlignment(0);
        labelDepartments.setHorizontalAlignment(0);
        textNameDeparment = new JTextField(20);
        JButton createDeparment = new JButton(
                new ButtonCreateDepartmentListener());
        Dimension dimension = new Dimension(590, 170);
        tableDepartments = new JTable(new MyTableModel());
        tableDepartments.setPreferredScrollableViewportSize(dimension);
        tableDepartments.setFillsViewportHeight(true);
        tableDepartments.setSelectionModel(new ForcedListSelectionModel());
        JScrollPane scrollPane = new JScrollPane(tableDepartments);
        JButton deleteDeparment = new JButton(
                new ButtonDeleteDepartmentListener());
        JPanel form = new JPanel(new FlowLayout(FlowLayout.CENTER, 999, 5));
        form.setPreferredSize(new Dimension(800, 400));
        form.add(labelNewDepartment);
        form.add(textNameDeparment);
        form.add(createDeparment);
        form.add(scrollPane);
        form.add(deleteDeparment);
        mainPanel.add(form, BorderLayout.CENTER);
        mainPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        this.setSize(800, 400);
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        updateTable();

    }

    private Boolean checkField() {

        Object[] options = { "Окей" };
        if (textNameDeparment.getText().equals("")) {
            JOptionPane.showOptionDialog(this,
                    "Поле \"Имя нового отдела\" пустое.", "Ошибка",
                    JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE, null,
                    options, options[0]);
            return false;
        } else if (checkDepartmentName(textNameDeparment.getText())) {
            JOptionPane
                    .showOptionDialog(
                            this,
                            "Данное название отдела уже существует в базе.\n Придумайте, пожалуйста, другое.",
                            "Ошибка", JOptionPane.OK_OPTION,
                            JOptionPane.ERROR_MESSAGE, null, options,
                            options[0]);

            return false;

        }
        return true;
    }

    private boolean checkDepartmentName(String name) {
        BasicDBObject user = (BasicDBObject) departments
                .findOne(new BasicDBObject("_id", name));
        if (user == null) {
            return false;
        }
        return true;
    }

    private void initConnection(MongoClient mongoClient) {
        mongo = mongoClient;
        db = mongo.getDB("documents");
        departments = db.getCollection("departments");
        users = db.getCollection("users");
        docs = mongo.getDB("documents").getCollection("docs");

    }

    private void createDepartment() {

        DBObject newDepartment;
        String name = textNameDeparment.getText();
        newDepartment = new BasicDBObject("_id", name).append("users",
                new Integer(0));
        departments.insert(newDepartment);
        Object[] options = { "Окей" };
        JOptionPane.showOptionDialog(this, "Был создан новый отдел \"" + name
                        + "\"", "Успех!", JOptionPane.OK_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
    }

    private void updateTable() {
        MyTableModel model = (MyTableModel) tableDepartments.getModel();
        if (departments.getCount() == 0) {
            model.empty();
        } else {
            model.fillTable(getDepartmentsArray());
        }
        tableDepartments.repaint();
    }

    private void deleteDepartment(String name) {
        BasicDBObject currentDepartment = new BasicDBObject("_id", name);
        departments.findAndRemove(currentDepartment);
        dismissAllDepartmentsUsers(name);
        makeGeneralAllDepartmentsDocuments(name);

        Object[] options = { "Окей" };
        JOptionPane.showOptionDialog(this, "Был удален отдел \"" + name + "\"",
                "Информация!", JOptionPane.OK_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
    }

    private void makeGeneralAllDepartmentsDocuments(String department) {
        BasicDBObject query = new BasicDBObject("department", department);
        docs.updateMulti(
                query,
                new BasicDBObject().append("$set",
                        new BasicDBObject().append("department", "general")));
    }

    private void dismissAllDepartmentsUsers(String department) {
        BasicDBObject query = new BasicDBObject("department", department);
        users.updateMulti(query,
                (DBObject) JSON.parse("{$unset : { chief: \"\" }}"));
        users.updateMulti(
                query,
                new BasicDBObject().append("$set",
                        new BasicDBObject().append("department", "general")));
    }

    private ArrayList<DBObject> getDepartmentsArray() {
        DBCursor cursor = departments.find();
        DBObject department;
        ArrayList<DBObject> departmentArray = new ArrayList<DBObject>();
        while (cursor.hasNext()) {
            department = cursor.next();
            departmentArray.add(department);
        }
        return departmentArray;
    }

    protected class ButtonCreateDepartmentListener extends AbstractAction {

        public ButtonCreateDepartmentListener() {
            putValue(Action.NAME, "Cоздать");
            putValue(Action.SHORT_DESCRIPTION, "Создать новое отделение");
        }

        public void actionPerformed(ActionEvent arg0) {

            if (checkField()) {
                createDepartment();
                updateTable();

            }

        }
    }

    protected class ButtonDeleteDepartmentListener extends AbstractAction {

        public ButtonDeleteDepartmentListener() {
            putValue(Action.NAME, "Удалить");
            putValue(Action.SHORT_DESCRIPTION, "Удалить существующее отделение");
        }

        public void actionPerformed(ActionEvent arg0) {
            if (tableDepartments.getSelectedRow() != -1) {
                String name = (String) tableDepartments.getModel().getValueAt(
                        tableDepartments.getSelectedRow(), 0);
                deleteDepartment(name);
                updateTable();
            }
        }

    }

    @SuppressWarnings("serial")
    class MyTableModel extends AbstractTableModel {

        private String[] columnNames = { "Название отдела", "Сотрудники" };

        public MyTableModel() {
            data = new Object[][] { { "", "" } };

        }

        public void fillTable(ArrayList<DBObject> array) {
            Integer size = array.size();
            data = new Object[size][2];
            int i = 0;
            DBObject currentDepartment;
            while (i < size) {
                currentDepartment = array.get(i);
                data[i][0] = currentDepartment.get("_id");
                data[i][1] = currentDepartment.get("users");
                i++;
            }
        }

        public void empty() {
            data = new Object[][] { { "", "" } };
        }

        private Object[][] data;

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.length;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            return data[row][col];
        }

        public Class<? extends Object> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public boolean isCellEditable(int row, int col) {

            return false;
        }

        public void setValueAt(Object value, int row, int col) {

            data[row][col] = value;
            fireTableCellUpdated(row, col);

        }

    }

    public class ForcedListSelectionModel extends DefaultListSelectionModel {

        public ForcedListSelectionModel() {
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }

        @Override
        public void clearSelection() {
        }

        @Override
        public void removeSelectionInterval(int index0, int index1) {
        }

    }

}
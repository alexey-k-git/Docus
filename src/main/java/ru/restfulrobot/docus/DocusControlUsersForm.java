package ru.restfulrobot.docus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

public class DocusControlUsersForm extends JFrame {

    private MongoClient mongo;
    private DB db;
    private DBCollection users;
    private DBCollection departments;
    private DocusFrame mainFrame;
    private JTable tableChiefs;
    private JTable tableUsers;
    private JButton buttonAppoint;
    private JButton buttonDismiss;
    private String department;
    private String[] items;

    public DocusControlUsersForm(MongoClient mongoClient, DocusFrame mainFrame) {
        super();
        initConnection(mongoClient);
        init(mainFrame);
    }

    public void init(DocusFrame docusFrame) {
        this.mainFrame = docusFrame;
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mainFrame.removeControlUserForm();
            }
        });
        Dimension dimension = new Dimension(350, 200);
        tableChiefs = new JTable(new ChiefsTableModel());
        tableChiefs.setPreferredScrollableViewportSize(dimension);
        tableChiefs.setFillsViewportHeight(true);
        tableChiefs.setSelectionModel(new ForcedListSelectionModel());
        dimension = new Dimension(200, 200);
        tableUsers = new JTable(new UsersTableModel());
        tableUsers.setPreferredScrollableViewportSize(dimension);
        tableUsers.setFillsViewportHeight(true);
        tableUsers.setSelectionModel(new ForcedListSelectionModel());
        JScrollPane scrollPaneUsers = new JScrollPane(tableUsers);
        JScrollPane scrollPaneChiefs = new JScrollPane(tableChiefs);
        buttonAppoint = new JButton(new ButtonAppointListener());
        buttonDismiss = new JButton(new ButtonDismissListener());
        items = getDepartments();
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JComboBox<?> box = (JComboBox<?>) e.getSource();
                department = (String) box.getSelectedItem();
            }
        };
        JComboBox<?> comboBox = new JComboBox<Object>(items);
        comboBox.setAlignmentX(CENTER_ALIGNMENT);
        comboBox.addActionListener(actionListener);
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.white);
        setTitle("Форма управления пользователями");
        this.setContentPane(mainPanel);
        JLabel labelChooseDepartment = new JLabel("Выбор подразделения:");
        JLabel labelOperation = new JLabel("Операции:");
        labelChooseDepartment.setHorizontalAlignment(0);
        labelOperation.setHorizontalAlignment(0);
        JPanel form = new JPanel(new FlowLayout(FlowLayout.CENTER, 999, 5));
        form.add(labelChooseDepartment);
        form.add(comboBox);
        form.add(labelOperation);
        form.add(buttonAppoint);
        form.add(buttonDismiss);
        mainPanel.add(scrollPaneUsers, BorderLayout.WEST);
        mainPanel.add(scrollPaneChiefs, BorderLayout.EAST);
        mainPanel.add(form, BorderLayout.CENTER);
        mainPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        this.setSize(800, 600);
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        updateTables();
    }

    private String[] getDepartments() {

        DBCursor cursor = departments.find();
        String departmentArray[] = new String[cursor.count()];
        int i = 0;
        DBObject department;
        while (cursor.hasNext()) {
            department = cursor.next();
            departmentArray[i] = department.get("_id").toString();
            i++;
        }
        if (departmentArray.length == 0) {
            buttonAppoint.setEnabled(false);
            buttonDismiss.setEnabled(false);
        } else {
            this.department = departmentArray[0];
        }
        return departmentArray;
    }

    private void initConnection(MongoClient mongoClient) {
        mongo = mongoClient;
        db = mongo.getDB("documents");
        users = db.getCollection("users");
        departments = db.getCollection("departments");
    }

    private void updateTables() {
        ((ChiefsTableModel) tableChiefs.getModel()).fillTable(getChiefsArray());
        ((UsersTableModel) tableUsers.getModel())
                .fillTable(getWorkersArray());
        tableUsers.repaint();
        tableChiefs.repaint();
    }

    private ArrayList<DBObject> getWorkersArray() {
        DBCursor cursor = users.find(new BasicDBObject("chief",
                new BasicDBObject("$exists", false)));
        DBObject worker;
        ArrayList<DBObject> workersArray = new ArrayList<DBObject>();
        while (cursor.hasNext()) {
            worker = cursor.next();
            workersArray.add(worker);
        }

        return workersArray;
    }

    private ArrayList<DBObject> getChiefsArray() {
        DBCursor cursor = users.find(new BasicDBObject("chief", true));
        DBObject chief;
        ArrayList<DBObject> chiefsArray = new ArrayList<DBObject>();
        while (cursor.hasNext()) {
            chief = cursor.next();
            chiefsArray.add(chief);
        }
        return chiefsArray;
    }

    private void appointUser(String name) {

        BasicDBObject currentUser = new BasicDBObject("_id", name);
        users.update(currentUser, new BasicDBObject().append(
                "$set",
                new BasicDBObject().append("department", department).append(
                        "chief", true)));
        Object[] options = { "Окей" };
        JOptionPane.showOptionDialog(this, "Теперь \"" + name
                        + "\" начальник подразделения \"" + department + "\"",
                "Информация!", JOptionPane.OK_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
    }

    private void addUserToDepartment() {
        BasicDBObject query = new BasicDBObject("_id", department);
        BasicDBObject currentDepartment = (BasicDBObject) departments
                .findOne(query);
        Integer currentCountUsers = currentDepartment.getInt("users");
        departments.update(query, new BasicDBObject().append("$set",
                new BasicDBObject().append("users", currentCountUsers + 1)));
    }

    private void removeUserFromDepartment(String department) {
        BasicDBObject query = new BasicDBObject("_id", department);
        BasicDBObject currentDepartment = (BasicDBObject) departments
                .findOne(query);
        Integer currentCountUsers = currentDepartment.getInt("users");
        departments.update(query, new BasicDBObject().append("$set",
                new BasicDBObject().append("users", currentCountUsers - 1)));
    }

    private void dismissChief(String name, String department) {

        BasicDBObject currentChief = new BasicDBObject("_id", name);
        users.update(currentChief,
                (DBObject) JSON.parse("{$unset : { chief: \"\" }}"));
        users.update(
                currentChief,
                new BasicDBObject().append("$set",
                        new BasicDBObject().append("department", "general")));
        Object[] options = { "Окей" };
        JOptionPane.showOptionDialog(this, "\"" + name
                        + "\" уволен со своего поста начальника  подразделения \""
                        + department + "\"", "Информация!", JOptionPane.OK_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
    }

    protected class ButtonAppointListener extends AbstractAction {

        public ButtonAppointListener() {
            putValue(Action.NAME, "<< назначить начальником");
            putValue(Action.SHORT_DESCRIPTION,
                    "Сделать данного пользователя начальником");
        }

        public void actionPerformed(ActionEvent arg0) {

            if (tableUsers.getSelectedRow() != -1) {
                String name = (String) tableUsers.getModel().getValueAt(
                        tableUsers.getSelectedRow(), 0);
                appointUser(name);
                addUserToDepartment();
                updateTables();
            }

        }

    }

    protected class ButtonDismissListener extends AbstractAction {

        public ButtonDismissListener() {
            putValue(Action.NAME, "уволить >>");
            putValue(Action.SHORT_DESCRIPTION, "Снять с должности");
        }

        public void actionPerformed(ActionEvent arg0) {

            if (tableChiefs.getSelectedRow() != -1) {
                String name = (String) tableChiefs.getModel().getValueAt(
                        tableChiefs.getSelectedRow(), 0);
                String department = (String) tableChiefs.getModel().getValueAt(
                        tableChiefs.getSelectedRow(), 1);
                dismissChief(name, department);
                removeUserFromDepartment(department);
                updateTables();
            }

        }
    }

    @SuppressWarnings("serial")
    class UsersTableModel extends AbstractTableModel {


        private String[] columnNames = { "Сотрудник", "Подразделение" };

        public UsersTableModel() {
            data = new Object[][] { { "", "" } };

        }

        public void fillTable(ArrayList<DBObject> array) {
            Integer size = array.size();
            if (size == 0 || size == 1) {
                data = new Object[][] { { "", "" } };
                buttonAppoint.setEnabled(false);
                return;
            }
            size -= 1;
            buttonAppoint.setEnabled(true);
            data = new Object[size][2];
            int i = 0;
            DBObject currentWorker;
            while (i < size) {
                currentWorker = array.get(i + 1);
                data[i][0] = currentWorker.get("_id");
                data[i][1] = currentWorker.get("department");
                i++;
            }
        }

        //старый метод

        /*
        public void fillTable(String[] array) {
            Integer size = array.length;
            if (size == 0) {
                data = new Object[][] { { "" } };
                buttonAppoint.setEnabled(false);
                return;
            }
            buttonAppoint.setEnabled(true);
            data = new Object[size][1];
            int i = 0;
            while (i < size) {
                data[i][0] = array[i];
                i++;
            }
        }
        */



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

    @SuppressWarnings("serial")
    class ChiefsTableModel extends AbstractTableModel {

        private String[] columnNames = { "Имя начальника", "Подразделение" };

        public ChiefsTableModel() {
            data = new Object[][] { { "", "" } };

        }

        public void fillTable(ArrayList<DBObject> array) {
            Integer size = array.size();
            if (size == 0 || size == 1) {
                data = new Object[][] { { "", "" } };
                buttonDismiss.setEnabled(false);
                return;
            }
            size -= 1;
            buttonDismiss.setEnabled(true);
            data = new Object[size][2];
            int i = 0;
            DBObject currentChief;
            while (i < size) {
                currentChief = array.get(i + 1);
                data[i][0] = currentChief.get("_id");
                data[i][1] = currentChief.get("department");
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

}
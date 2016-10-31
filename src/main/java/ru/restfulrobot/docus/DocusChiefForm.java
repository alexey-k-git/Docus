package ru.restfulrobot.docus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;

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
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

public class DocusChiefForm extends JFrame {

    private MongoClient mongo;
    private DB db;
    private DBCollection users;
    private DBCollection departments;
    private DocusFrame mainFrame;
    private JTable tableWorkers;
    private JTable tableFreeUsers;
    private JButton buttonAppoint;
    private JButton buttonDismiss;
    private String department;

    public DocusChiefForm(MongoClient mongoClient, DocusFrame mainFrame,
                          String department) {
        super();
        initConnection(mongoClient);
        init(mainFrame, department);
    }

    public void init(DocusFrame docusFrame, String department) {
        this.department = department;
        this.mainFrame = docusFrame;
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mainFrame.removeChiefForm();
            }
        });
        Dimension dimension = new Dimension(200, 200);
        tableWorkers = new JTable(new WorkersTableModel());
        tableWorkers.setPreferredScrollableViewportSize(dimension);
        tableWorkers.setFillsViewportHeight(true);
        tableWorkers.setSelectionModel(new ForcedListSelectionModel());
        tableFreeUsers = new JTable(new UsersTableModel());
        tableFreeUsers.setPreferredScrollableViewportSize(dimension);
        tableFreeUsers.setFillsViewportHeight(true);
        tableFreeUsers.setSelectionModel(new ForcedListSelectionModel());
        JScrollPane scrollPaneUsers = new JScrollPane(tableFreeUsers);
        JScrollPane scrollPaneChiefs = new JScrollPane(tableWorkers);
        buttonAppoint = new JButton(new ButtonAppointListener());
        buttonDismiss = new JButton(new ButtonDismissListener());
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.white);
        setTitle("Форма управления своим подразделением");
        this.setContentPane(mainPanel);
        JLabel labelDepartment = new JLabel("Ваше подразделение:");
        JLabel labelDepartmentValue = new JLabel(department);
        JLabel labelOperation = new JLabel("Операции:");
        labelDepartment.setHorizontalAlignment(0);
        labelOperation.setHorizontalAlignment(0);
        JPanel form = new JPanel(new FlowLayout(FlowLayout.CENTER, 999, 5));
        form.add(labelDepartment);
        form.add(labelDepartmentValue);
        form.add(labelOperation);
        form.add(buttonAppoint);
        form.add(buttonDismiss);
        mainPanel.add(scrollPaneUsers, BorderLayout.WEST);
        mainPanel.add(scrollPaneChiefs, BorderLayout.EAST);
        mainPanel.add(form, BorderLayout.CENTER);
        mainPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        this.setSize(600, 600);
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        updateTables();
    }

    private void initConnection(MongoClient mongoClient) {
        mongo = mongoClient;
        db = mongo.getDB("documents");
        users = db.getCollection("users");
        departments = db.getCollection("departments");
    }

    private void updateTables() {
        ((WorkersTableModel) tableWorkers.getModel())
                .fillTable(getWorkersArray());
        ((UsersTableModel) tableFreeUsers.getModel())
                .fillTable(getFreeUsersArray());
        tableFreeUsers.repaint();
        tableWorkers.repaint();
    }

    private String[] getFreeUsersArray() {
        DBCursor cursor = users.find(new BasicDBObject("department", "general")
                .append("chief", new BasicDBObject("$exists", false)));
        String users[] = new String[cursor.count()];
        int i = 0;
        DBObject user;
        while (cursor.hasNext()) {
            user = cursor.next();
            users[i] = user.get("_id").toString();
            i++;
        }
        return users;
    }

    private String[] getWorkersArray() {
        DBCursor cursor = users
                .find(new BasicDBObject("department", department).append(
                        "chief", new BasicDBObject("$exists", false)));
        String users[] = new String[cursor.count()];
        int i = 0;
        DBObject user;
        while (cursor.hasNext()) {
            user = cursor.next();
            users[i] = user.get("_id").toString();
            i++;
        }
        return users;
    }

    private void appointUser(String name) {

        BasicDBObject currentUser = new BasicDBObject("_id", name);
        users.update(
                currentUser,
                new BasicDBObject().append("$set",
                        new BasicDBObject().append("department", department)));
        Object[] options = { "Окей" };
        JOptionPane.showOptionDialog(this, "Теперь \"" + name
                        + "\" работает в подразделении \"" + department + "\"",
                "Информация!", JOptionPane.OK_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
    }

    private void dismissWorker(String name) {

        BasicDBObject currentChief = new BasicDBObject("_id", name);
        users.update(
                currentChief,
                new BasicDBObject().append("$set",
                        new BasicDBObject().append("department", "general")));
        Object[] options = { "Окей" };
        JOptionPane.showOptionDialog(this, "\"" + name
                        + "\" уволен из подразделения \"" + department + "\"",
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

    private void removeUserFromDepartment() {
        BasicDBObject query = new BasicDBObject("_id", department);
        BasicDBObject currentDepartment = (BasicDBObject) departments
                .findOne(query);
        Integer currentCountUsers = currentDepartment.getInt("users");
        departments.update(query, new BasicDBObject().append("$set",
                new BasicDBObject().append("users", currentCountUsers - 1)));
    }

    protected class ButtonAppointListener extends AbstractAction {

        public ButtonAppointListener() {
            putValue(Action.NAME, "<< сделать сотрудником");
            putValue(Action.SHORT_DESCRIPTION,
                    "Сделать данного пользователя свои сотрудником");
        }

        public void actionPerformed(ActionEvent arg0) {

            if (tableFreeUsers.getSelectedRow() != -1) {
                String name = (String) tableFreeUsers.getModel().getValueAt(
                        tableFreeUsers.getSelectedRow(), 0);
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

            if (tableWorkers.getSelectedRow() != -1) {
                String name = (String) tableWorkers.getModel().getValueAt(
                        tableWorkers.getSelectedRow(), 0);
                dismissWorker(name);
                removeUserFromDepartment();
                updateTables();
            }

        }
    }

    @SuppressWarnings("serial")
    class UsersTableModel extends AbstractTableModel {

        private String[] columnNames = { "Свободные пользователи" };

        public UsersTableModel() {
            data = new Object[][] { { "" } };

        }

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
    class WorkersTableModel extends AbstractTableModel {

        private String[] columnNames = { "Подчиненные" };

        public WorkersTableModel() {
            data = new Object[][] { { "", "" } };

        }

        public void fillTable(String[] array) {

            Integer size = array.length;
            if (size == 0) {
                data = new Object[][] { { "" } };
                buttonDismiss.setEnabled(false);
                return;
            }
            buttonDismiss.setEnabled(true);
            data = new Object[size][1];
            int i = 0;
            while (i < size) {
                data[i][0] = array[i];
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
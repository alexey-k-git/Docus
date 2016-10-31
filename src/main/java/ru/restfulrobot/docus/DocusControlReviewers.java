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
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import ru.restfulrobot.docus.DocTree.Document;
import ru.restfulrobot.docus.DocusAuthorizationForm.ButtonEnterListener;
import ru.restfulrobot.docus.DocusControlDepartmentForm.ForcedListSelectionModel;
import ru.restfulrobot.docus.DocusControlDepartmentForm.MyTableModel;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

// Форма управления правами Модераторов
public class DocusControlReviewers extends JFrame {

    private MongoClient mongo;
    private DB db;
    private DBCollection users;
    private DBCollection departments;
    private DocusFrame mainFrame;
    private JTable tableReviewers;
    private JTable tableUsers;
    private JTable tableDepartments;
    private JButton buttonAppoint;
    private JButton buttonDismiss;
    private JButton buttonAddDepartment;
    private JButton buttonDeleteDepartment;
    private JList listReviewerDepartments;
    private DefaultListModel listModel;

    public DocusControlReviewers(MongoClient mongoClient, DocusFrame mainFrame) {
        super();
        initConnection(mongoClient);
        init(mainFrame);
    }

    public void init(DocusFrame docusFrame) {
        this.mainFrame = docusFrame;
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mainFrame.removeControlReviewerForm();
            }
        });
        //размеры элементов
        Dimension dimension = new Dimension(200, 200);
        //таблица с модераторами
        tableReviewers = new JTable(new ReviewersTableModel());
        tableReviewers.setPreferredScrollableViewportSize(dimension);
        tableReviewers.setFillsViewportHeight(true);
        tableReviewers.setSelectionModel(new ForcedListSelectionModel());
        tableReviewers.getSelectionModel().addListSelectionListener(new SelectionListener());
        //таблица с подразделениями
        tableDepartments = new JTable(new DepartmentsTableModel());
        tableDepartments.setPreferredScrollableViewportSize(dimension);
        tableDepartments.setFillsViewportHeight(true);
        tableDepartments.setSelectionModel(new ForcedListSelectionModel());
        //меняем размер
        dimension = new Dimension(300, 200);
        //таблица юзеров
        tableUsers = new JTable(new UsersTableModel());
        tableUsers.setPreferredScrollableViewportSize(dimension);
        tableUsers.setFillsViewportHeight(true);
        tableUsers.setSelectionModel(new ForcedListSelectionModel());
        //кнопки со слушателями
        buttonAppoint = new JButton(new ButtonAppointListener());
        buttonDismiss = new JButton(new ButtonDismissListener());
        buttonDeleteDepartment = new JButton(new ButtonDeleteDepartmentListener());
        buttonAddDepartment = new JButton(new ButtonAddDepartmentListener());

        listModel = new DefaultListModel();
        listReviewerDepartments = new JList(listModel);
        JScrollPane scrollPanelUsers = new JScrollPane(tableUsers);
        JScrollPane scrollPanelReviewers = new JScrollPane(tableReviewers);
        JScrollPane scrollPanelDepartments = new JScrollPane(tableDepartments);
        JScrollPane scrollDepartmentsList = new JScrollPane(listReviewerDepartments);
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.white);
        setTitle("Форма управления модераторами");
        this.setContentPane(mainPanel);
        JLabel labelOperation = new JLabel("Операции:");
        JLabel labelDepartments = new JLabel("Текущие обязаности:");
        JLabel labelControlDepartment = new JLabel(
                "Подразделения для добавления:");
        labelOperation.setHorizontalAlignment(0);
        labelDepartments.setHorizontalAlignment(0);
        JPanel form = new JPanel(new FlowLayout(FlowLayout.CENTER, 999, 5));
        form.add(labelOperation);
        form.add(buttonAppoint);
        form.add(buttonDismiss);
        form.add(labelDepartments);
        form.add(scrollPanelDepartments);
        form.add(buttonDeleteDepartment);
        form.add(labelControlDepartment);
        form.add(scrollDepartmentsList);
        form.add(buttonAddDepartment);
        mainPanel.add(scrollPanelUsers, BorderLayout.WEST);
        mainPanel.add(scrollPanelReviewers, BorderLayout.EAST);
        mainPanel.add(form, BorderLayout.CENTER);
        mainPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        this.setSize(800, 600);
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

    //Обновить таблицы со списком немодераторов и модераторов
    private void updateTables() {
        ((ReviewersTableModel) tableReviewers.getModel())
                .fillTable(getReviwersArray());
        ((UsersTableModel) tableUsers.getModel()).fillTable(getUsersArray());
        tableUsers.repaint();
        tableReviewers.repaint();
    }

    //Заполнить подразделения, которые можно назначить на данного Модератора
    private void fillListFreeDepartmentForThisReviewer(
            String[] existingDepartmens) {
        listModel.clear();
        DBCursor cursor = departments.find(new BasicDBObject("_id",
                new BasicDBObject("$nin", existingDepartmens)));
        if (cursor.count() == 0) {
            buttonAddDepartment.setEnabled(false);
            return;
        }
        buttonAddDepartment.setEnabled(true);
        while (cursor.hasNext()) {
            listModel.addElement(cursor.next().get("_id").toString());
        }
    }

    //Получить массив немодераторов
    private ArrayList<DBObject> getUsersArray() {
        DBCursor cursor = users.find(new BasicDBObject("department",
                new BasicDBObject("$ne", "reviewers")));
        DBObject user;
        //админа конечно пропускаем следющей строчкой
        cursor.next();
        ArrayList<DBObject> usersArray = new ArrayList<DBObject>();
        while (cursor.hasNext()) {
            user = cursor.next();
            usersArray.add(user);
        }
        return usersArray;

    }

    //Получить массив модераторов
    private String[] getReviwersArray() {
        DBCursor cursor = users.find(new BasicDBObject("department",
                "reviewers"));
        String reviewers[] = new String[cursor.count()];
        int i = 0;
        DBObject reviewer;
        while (cursor.hasNext()) {
            reviewer = cursor.next();
            reviewers[i] = reviewer.get("_id").toString();
            i++;
        }
        return reviewers;
    }

    // Повышаем в должности данного пользователя до модератора
    private void appointUser(String name) {

        BasicDBObject currentUser = new BasicDBObject("_id", name);
        users.update(currentUser,
                (DBObject) JSON.parse("{$unset : { chief: \"\" }}"));
        users.update(currentUser, new BasicDBObject().append(
                "$set",
                new BasicDBObject().append("department", "reviewers").append(
                        "controls", new String[] { "general" })));

        Object[] options = { "Окей" };
        JOptionPane.showOptionDialog(this, "Теперь \"" + name
                        + "\" модератор \"", "Информация!", JOptionPane.OK_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
    }

    //Теперь кол-во пользователей в определенном департменте стало меньше...
    private void removeUserFromDepartment(String department) {
        if (!department.equals("general")) {
            BasicDBObject query = new BasicDBObject("_id", department);
            BasicDBObject currentDepartment = (BasicDBObject) departments
                    .findOne(query);
            Integer currentCountUsers = currentDepartment.getInt("users");
            departments
                    .update(query, new BasicDBObject().append("$set",
                            new BasicDBObject().append("users",
                                    currentCountUsers - 1)));
        }

    }

    // Увольняем модератора. Теперь он свободный как ветер работник в отделе general
    private void dismissReviewer(String name) {

        BasicDBObject currentChief = new BasicDBObject("_id", name);

        users.update(
                currentChief,
                new BasicDBObject().append("$set",
                        new BasicDBObject().append("department", "general")));
        users.update(
                currentChief,
                new BasicDBObject().append("$unset",
                        new BasicDBObject().append("controls", "")));

        Object[] options = { "Окей" };
        JOptionPane.showOptionDialog(this, "\"" + name
                        + "\" больше не модератор", "Информация!",
                JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                options, options[0]);
    }

    private void addDepartmentToThisReviewer(String name,
                                             List<String> listDepartmentsForAdd) {
        BasicDBObject currentReviewer = (BasicDBObject) users
                .findOne(new BasicDBObject("_id", name));
        BasicDBList departmentList = (BasicDBList) currentReviewer
                .get("controls");
        String departments[] = new String[departmentList.size()
                + listDepartmentsForAdd.size()];
        int i = 0;
        for (Iterator<Object> it = departmentList.iterator(); it.hasNext();) {
            departments[i] = (String.valueOf(it.next()));
            i++;
        }
        for (int j = 0; i < departments.length; i++, j++) {
            departments[i] = listDepartmentsForAdd.get(j);
        }
        users.update(
                currentReviewer,
                new BasicDBObject().append("$set",
                        new BasicDBObject().append("controls", departments)));
    }

    private void deleteCurrentDepartmentFromThisReviewer(String name,
                                                         String department) {
        BasicDBObject currentReviewer = (BasicDBObject) users
                .findOne(new BasicDBObject("_id", name));
        BasicDBList departmentList = (BasicDBList) currentReviewer
                .get("controls");
        String departments[] = new String[departmentList.size() - 1];
        int i = 0;
        String currentDepartment;
        for (Iterator<Object> it = departmentList.iterator(); it.hasNext();) {
            currentDepartment = String.valueOf(it.next());
            if (!department.equals(currentDepartment)) {
                departments[i] = currentDepartment;
            }
            i++;
        }
        users.update(
                currentReviewer,
                new BasicDBObject().append("$set",
                        new BasicDBObject().append("controls", departments)));
    }

    //получить массив департаментов для даннного модератора
    private String[] getReviewerDepartmentsArray(String name) {

        BasicDBObject currentReviewer = (BasicDBObject) users
                .findOne(new BasicDBObject("_id", name));
        BasicDBList departmentList = (BasicDBList) currentReviewer
                .get("controls");
        String departments[] = new String[departmentList.size() - 1];
        int i = 0;
        Iterator<Object> it = departmentList.iterator();
        it.next();
        while(it.hasNext())
        {
            departments[i] = (String.valueOf(it.next()));
            i++;
        }
        return departments;
    }

    //Обновить таблицу подчиненных подразделений для текущего Пользователя
    private void updateReviewerTables() {
        //Достаем текущего модератора
        String name = (String) tableReviewers.getModel().getValueAt(
                tableReviewers.getSelectedRow(), 0);
        //получаем массив подразделений для данного модератора
        String[] reviewerDepartmentsArray = getReviewerDepartmentsArray(name);
        ((DepartmentsTableModel) tableDepartments.getModel()).fillTable(reviewerDepartmentsArray);
        tableDepartments.repaint();
        //заполнить таблицу подразделениями, которые можно назначить на данного Модератора
        fillListFreeDepartmentForThisReviewer(reviewerDepartmentsArray);

    }

    // Листенер для кнопки назначения пользователя на должность модератора
    protected class ButtonAppointListener extends AbstractAction {

        public ButtonAppointListener() {
            putValue(Action.NAME, "<< назначить модератором");
            putValue(Action.SHORT_DESCRIPTION,
                    "Сделать данного пользователя модератором");
        }

        public void actionPerformed(ActionEvent arg0) {
            if (tableUsers.getSelectedRow() != -1) {
                TableModel model = tableUsers.getModel();
                Integer row = tableUsers.getSelectedRow();
                String name = (String) model.getValueAt(row, 0);
                String department = (String) model.getValueAt(row, 1);
                appointUser(name);
                removeUserFromDepartment(department);
                updateTables();
            }

        }

    }

    // Листенер для кнопки увольнения модератора
    protected class ButtonDismissListener extends AbstractAction {

        public ButtonDismissListener() {
            putValue(Action.NAME, "уволить >>");
            putValue(Action.SHORT_DESCRIPTION, "Снять с должности");
        }

        public void actionPerformed(ActionEvent arg0) {

            if (tableReviewers.getSelectedRow() != -1) {
                String name = (String) tableReviewers.getModel().getValueAt(
                        tableReviewers.getSelectedRow(), 0);
                dismissReviewer(name);
                updateTables();
            }

        }
    }

    // Листенер для поручения модератору нового департамента
    protected class ButtonAddDepartmentListener extends AbstractAction {

        public ButtonAddDepartmentListener() {
            putValue(Action.NAME, "Добавить");
            putValue(Action.SHORT_DESCRIPTION,
                    "Поручить модератору новые подразделения ");
        }

        public void actionPerformed(ActionEvent arg0) {
            List<String> listDepartmentsForAdd =
                    (List<String> ) listReviewerDepartments.getSelectedValuesList();
            if (listDepartmentsForAdd.size() != 0) {
                String name = (String) tableReviewers.getModel().getValueAt(
                        tableReviewers.getSelectedRow(), 0);
                addDepartmentToThisReviewer(name, listDepartmentsForAdd);
                updateReviewerTables();
            }

        }

    }

    protected class ButtonDeleteDepartmentListener extends AbstractAction {

        public ButtonDeleteDepartmentListener() {
            putValue(Action.NAME, "Удалить");
            putValue(Action.SHORT_DESCRIPTION,
                    "Снять с модератора данное подразделение");
        }

        public void actionPerformed(ActionEvent arg0) {

            if (tableDepartments.getSelectedRow() != -1) {
                String department = (String) tableDepartments.getModel()
                        .getValueAt(tableDepartments.getSelectedRow(), 0);
                if (department.equals(""))
                {
                    return;
                }
                String name = (String) tableReviewers.getModel().getValueAt(
                        tableReviewers.getSelectedRow(), 0);

                deleteCurrentDepartmentFromThisReviewer(name, department);
                updateReviewerTables();
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
            if (size == 0) {
                data = new Object[][] { { "", "" } };
                buttonAppoint.setEnabled(false);
                return;
            }
            buttonAppoint.setEnabled(true);
            data = new Object[size][2];
            int i = 0;
            DBObject currentChief;
            while (i < size) {
                currentChief = array.get(i);
                data[i][0] = currentChief.get("_id");
                data[i][1] = currentChief.get("department");
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

    @SuppressWarnings("serial")
    class DepartmentsTableModel extends AbstractTableModel {

        private String[] columnNames = { "Подразделения" };

        public DepartmentsTableModel() {
            data = new Object[][] { { "", "" } };
        }


        public void fillTable(String[] array) {
            Integer size = array.length;
            if (size == 0) {
                data = new Object[][] { { "" } };
                buttonDeleteDepartment.setEnabled(false);
                return;
            }
            buttonDeleteDepartment.setEnabled(true);
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

    @SuppressWarnings("serial")
    class ReviewersTableModel extends AbstractTableModel {

        private String[] columnNames = { "Модератор" };

        public ReviewersTableModel() {
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

    class SelectionListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
                updateReviewerTables();
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

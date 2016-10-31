package ru.restfulrobot.docus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.bson.types.ObjectId;

import ru.restfulrobot.docus.DocTree.Document;
import ru.restfulrobot.docus.DocusFrame.AccesToFile;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import com.mongodb.util.JSON;

public class DocusReviewerForm extends JFrame {

    private static final String ACTION_RESET = "reset";
    private static final String ACTION_SAVE = "save";
    private static final String ACTION_LOADFILE = "loadFile";
    private static final String ACTION_SAVEFILE = "saveFile";
    private static final String ACTION_DELETEFILE = "deleteFile";
    private static final String ACTION_VIEWDOC = "viewDoc";
    private static final String ACTION_PUBLISHDOC = "publishDoc";
    private static final String ACTION_DELETEDOC = "deleteDoc";

    private MongoClient mongo;
    private DB db;
    private DBCollection docs;
    private DocusFrame mainFrame;
    private JTable tableDocuments;
    private JButton buttonView;
    private JButton buttonRemove;
    private DocumentViewer documentViewer;
    private Map<String, Action> actions = new HashMap<String, Action>();
    private JFrame thisFrame;
    private DocTree docTree;
    private ObjectId currentDocId;
    private Boolean anyChanges;
    private JComboBox<?> comboBox;

    public DocusReviewerForm(MongoClient mongoClient, DocusFrame mainFrame,
                             String[] departments, DocTree docTree) {
        super();
        initConnection(mongoClient);
        init(mainFrame, departments, docTree);
        mainFrame.setEnabled(false);
    }

    private void updateTable() {
        String department = (String) comboBox.getSelectedItem();
        updateDocumentTable(department);
    }

    public void init(DocusFrame docusFrame, String[] departments,
                     DocTree docTree) {
        anyChanges = false;
        this.docTree = docTree;
        thisFrame = this;
        this.mainFrame = docusFrame;
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mainFrame.removeReviewerForm(anyChanges);
                mainFrame.setEnabled(true);
            }
        });
        tableDocuments = new JTable(new DocumentsTableModel());
        tableDocuments.setPreferredScrollableViewportSize(new Dimension(500,
                370));
        tableDocuments.setFillsViewportHeight(true);
        tableDocuments.setSelectionModel(new ForcedListSelectionModel());
        JScrollPane scrollPanelDocuments = new JScrollPane(tableDocuments);
        String[] items = departments;
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox) e.getSource();
                String department = (String) cb.getSelectedItem();
                updateDocumentTable(department);
            }
        };
        comboBox = new JComboBox<Object>(items);
        comboBox.setAlignmentX(LEFT_ALIGNMENT);
        comboBox.addActionListener(actionListener);
        buttonView = new JButton(new ButtonViewListener());
        buttonRemove = new JButton(new ButtonRemoveListener());
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.white);
        setTitle("Форма модерирования");
        this.setContentPane(mainPanel);
        JLabel labelDepartment = new JLabel("Выбор подразделения:");
        JLabel labelDocumentsTable = new JLabel("Документы к проверке:");
        JLabel labelOperation = new JLabel("Операции:");
        labelDepartment.setHorizontalAlignment(0);
        labelOperation.setHorizontalAlignment(0);
        JPanel form = new JPanel(new FlowLayout(FlowLayout.CENTER, 999, 5));
        form.add(labelDepartment);
        form.add(comboBox);
        form.add(labelDocumentsTable);
        form.add(scrollPanelDocuments);
        form.add(labelOperation);
        form.add(buttonView);
        form.add(buttonRemove);
        mainPanel.add(form, BorderLayout.CENTER);
        mainPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        updateDocumentTable(departments[0]);
        createActions();
        this.setSize(600, 600);
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    private void createActions() {
        actions.put(ACTION_VIEWDOC, new ButtonViewListener());
        actions.put(ACTION_DELETEDOC, new ButtonRemoveListener());

    }

    private void initConnection(MongoClient mongoClient) {
        mongo = mongoClient;
        db = mongo.getDB("documents");
        docs = db.getCollection("docs");
    }

    private void updateDocumentTable(String department) {
        DBCursor cursor = docs.find(new BasicDBObject("draft", true).append(
                "department", department));
        BasicDBObject document;
        ArrayList<DBObject> documentsArray = new ArrayList<DBObject>();
        String parentName;
        while (cursor.hasNext()) {
            document = (BasicDBObject) cursor.next();
            parentName = docs.findOne(document.getObjectId("parent"))
                    .get("name").toString();
            document.append("parentName", parentName);
            documentsArray.add(document);
        }
        ((DocumentsTableModel) tableDocuments.getModel())
                .fillTable(documentsArray);
        tableDocuments.repaint();
    }

    private int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(l
                    + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    private void deleteDocument(ObjectId currentDocId) {
        BasicDBObject query = new BasicDBObject("_id", currentDocId);
        docs.findAndRemove(query);
    }

    private void closeChildWindow() {
        thisFrame.setEnabled(true);
        documentViewer = null;
    }

    class DocumentViewer extends JFrame {
        private Boolean changedFields;
        private JTextField textName;
        private JTextField textAuthor;
        private JTextArea textTags;
        private JLabel modified;
        private JLabel created;
        private JLabel labelDepartmentValue;
        private JLabel statusFile;
        private String name;
        private String author;
        private String tags;

        public DocumentViewer() {
            super();
            thisFrame.setEnabled(false);
            this.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    closeChildWindow();
                }
            });
            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.setBackground(Color.white);
            this.setTitle("Окно модерирования документа");
            this.setContentPane(mainPanel);
            createActions();
            mainPanel.add(createEditForm(), BorderLayout.CENTER);
            this.setSize(620, 600);
            this.setResizable(true);
            this.setLocationRelativeTo(null);
            this.setVisible(true);

        }

        private void textsChanges() {
            changedFields = true;
            actions.get(ACTION_SAVE).setEnabled(true);
            actions.get(ACTION_RESET).setEnabled(true);
        }

        private void createActions() {
            actions.put(ACTION_RESET, new ButtonResetListener());
            actions.put(ACTION_SAVE, new ButtonSaveListener());
            actions.put(ACTION_SAVEFILE, new ButtonSaveFileListener());
            actions.put(ACTION_LOADFILE, new ButtonLoadFileListener());
            actions.put(ACTION_DELETEFILE, new ButtonDeleteFileListener());
            actions.put(ACTION_PUBLISHDOC, new ButtonPublishListener());
        }

        private JPanel createEditForm() {
            JButton save = new JButton(actions.get(ACTION_SAVE));
            JButton cancel = new JButton("Отмена");
            cancel.setEnabled(false);
            JButton reset = new JButton(actions.get(ACTION_RESET));
            JButton loadFile = new JButton(actions.get(ACTION_LOADFILE));
            JButton deleteFile = new JButton(actions.get(ACTION_DELETEFILE));
            JButton saveFile = new JButton(actions.get(ACTION_SAVEFILE));
            JButton publish = new JButton(actions.get(ACTION_PUBLISHDOC));
            JButton delete = new JButton(actions.get(ACTION_DELETEDOC));
            JLabel label = new JLabel("Форма редактирования:");
            label.setHorizontalAlignment(0);
            JLabel labelName = new JLabel("Название документа:");
            JLabel labelAuthor = new JLabel("Автор:");
            JLabel labelTags = new JLabel("Ключевые слова:");
            JLabel labelCreated = new JLabel("Дата создания:");
            JLabel labelModified = new JLabel("Дата последнего изменения:");
            JLabel labelControl = new JLabel("Управление файлом");
            JLabel labelDepartment = new JLabel("Подразделение:");
            JPanel formPanel = new JPanel(new BorderLayout());
            JPanel form = new JPanel(new FlowLayout(FlowLayout.CENTER, 999, 4));
            form.setPreferredSize(new Dimension(300, 500));
            JPanel buttons = new JPanel(new GridLayout(0, 3, 0, 0));
            JPanel saveLoadFile = new JPanel(new FlowLayout(FlowLayout.CENTER));
            JPanel action = new JPanel(new FlowLayout(FlowLayout.CENTER));

            created = new JLabel("-");
            modified = new JLabel("-");
            statusFile = new JLabel("-");
            labelDepartmentValue = new JLabel("-");

            saveLoadFile.add(loadFile);
            saveLoadFile.add(deleteFile);
            saveLoadFile.add(saveFile);
            buttons.add(save);
            buttons.add(reset);
            buttons.add(cancel);
            action.add(publish);
            action.add(delete);

            textName = new JTextField(30);
            textName.setBorder(BorderFactory.createLineBorder(Color.black));
            textName.setDisabledTextColor(Color.black);
            textName.getDocument()
                    .addDocumentListener(new MyDocumentListener());
            textAuthor = new JTextField(30);
            textAuthor.setBorder(BorderFactory.createLineBorder(Color.black));
            textAuthor.setDisabledTextColor(Color.black);
            textAuthor.getDocument().addDocumentListener(
                    new MyDocumentListener());
            textTags = new JTextArea(5, 30);
            textTags.setWrapStyleWord(true);
            textTags.setLineWrap(true);
            textTags.setBorder(BorderFactory.createLineBorder(Color.black));
            textTags.setDisabledTextColor(Color.black);
            textTags.getDocument()
                    .addDocumentListener(new MyDocumentListener());
            form.add(labelName);
            form.add(textName);
            form.add(labelAuthor);
            form.add(textAuthor);
            form.add(labelTags);
            form.add(textTags);
            form.add(labelCreated);
            form.add(created);
            form.add(labelModified);
            form.add(modified);
            form.add(labelControl);
            form.add(statusFile);
            form.add(saveLoadFile);
            form.add(labelDepartment);
            form.add(labelDepartmentValue);
            form.add(buttons);
            formPanel.add(form, BorderLayout.CENTER);
            formPanel.add(action, BorderLayout.SOUTH);
            formPanel.add(label, BorderLayout.NORTH);
            fillForm(getCurrentDocumentInDBFormat());
            changedFields = false;
            actions.get(ACTION_SAVE).setEnabled(false);
            actions.get(ACTION_RESET).setEnabled(false);
            return formPanel;
        }

        private void fillForm(BasicDBObject currentDocumentInDBFormat) {
            name = currentDocumentInDBFormat.getString("name");
            textName.setText(name);
            author = currentDocumentInDBFormat.getString("author");
            textAuthor.setText(author);
            created.setText(currentDocumentInDBFormat.getDate("created")
                    .toString());
            modified.setText(currentDocumentInDBFormat.getDate("modified")
                    .toString());
            labelDepartmentValue.setText(currentDocumentInDBFormat
                    .getString("department"));
            tags = "";
            BasicDBList departmentList = (BasicDBList) currentDocumentInDBFormat
                    .get("tags");
            for (Iterator<Object> it = departmentList.iterator(); it.hasNext();) {
                tags += String.valueOf(it.next());
                if (it.hasNext()) {
                    tags += ", ";
                }
            }
            textTags.setText(tags);
            if (currentDocumentInDBFormat.containsField("file")) {
                statusFile.setText(currentDocumentInDBFormat.getString("file"));
                actions.get(ACTION_DELETEFILE).setEnabled(true);
                actions.get(ACTION_LOADFILE).setEnabled(true);
                actions.get(ACTION_SAVEFILE).setEnabled(true);

            } else {
                statusFile.setText("У данного документа отсутствует файл");
                actions.get(ACTION_DELETEFILE).setEnabled(false);
                actions.get(ACTION_LOADFILE).setEnabled(false);
                actions.get(ACTION_SAVEFILE).setEnabled(true);
            }

        }

        private BasicDBObject getCurrentDocumentInDBFormat() {

            BasicDBObject query = new BasicDBObject("_id", currentDocId);
            BasicDBObject currentDocument = (BasicDBObject) docs.findOne(query);
            return currentDocument;
        }

        private void publishDocument() {

            BasicDBObject query = new BasicDBObject("_id", currentDocId);
            BasicDBObject currentDocument = (BasicDBObject) docs.findOne(query);
            DBCursor cursor = docs.find(new BasicDBObject("parent",
                    currentDocument.getObjectId("parent")).append("draft",
                    false));
            Integer countNormalChilds = cursor.size();
            BasicDBObject changeFieldDB = new BasicDBObject();
            changeFieldDB.append(
                    "$set",
                    new BasicDBObject().append("draft", false).append("order",
                            countNormalChilds));
            docs.update(query, changeFieldDB);
        }

        private boolean emptyFields() {
            if (textName.getText().equals("")
                    || textAuthor.getText().equals("")
                    || textTags.getText().equals("")) {
                return true;
            }
            return false;
        }

        private void saveDocument() {
            List<String> tags = new ArrayList<String>();
            Scanner s = new Scanner(textTags.getText()).useDelimiter(",\\s*");
            while (s.hasNext()) {
                tags.add(s.next());
            }
            s.close();
            BasicDBObject queryDB = new BasicDBObject();
            BasicDBObject changeFieldDB = new BasicDBObject();
            queryDB.put("_id", currentDocId);
            changeFieldDB.append(
                    "$set",
                    new BasicDBObject().append("author", textAuthor.getText())
                            .append("name", textName.getText())
                            .append("tags", tags)
                            .append("modified", new Date()));
            docs.update(queryDB, changeFieldDB);
        }

        protected class ButtonResetListener extends AbstractAction {

            public ButtonResetListener() {
                putValue(Action.NAME, "Сбросить");
                putValue(Action.SHORT_DESCRIPTION, "Сброс изменений");
            }

            @Override
            public void actionPerformed(ActionEvent e) {

                textName.setText(name);
                textAuthor.setText(author);
                textTags.setText(tags);
                changedFields = false;
                actions.get(ACTION_SAVE).setEnabled(false);
                actions.get(ACTION_RESET).setEnabled(false);
            }

        }

        protected class ButtonSaveListener extends AbstractAction {

            public ButtonSaveListener() {
                putValue(Action.NAME, "Сохранить");
                putValue(Action.SHORT_DESCRIPTION, "Сохранить изменения");
            }

            public void actionPerformed(ActionEvent arg0) {

                if (emptyFields()) {
                    Object[] options = { "Окей" };
                    JOptionPane
                            .showOptionDialog(
                                    documentViewer,
                                    "В полях документа были оcтавлены пустые поля.\n Сохранение не будет произведено",
                                    "Предупреждение", JOptionPane.OK_OPTION,
                                    JOptionPane.ERROR_MESSAGE, null, options,
                                    options[0]);

                } else {
                    saveDocument();
                    fillForm(getCurrentDocumentInDBFormat());
                    Object[] options = { "Окей" };
                    JOptionPane.showOptionDialog(documentViewer,
                            "Документ был успешно сохранён", "Успех",
                            JOptionPane.OK_OPTION,
                            JOptionPane.INFORMATION_MESSAGE, null, options,
                            options[0]);
                    updateTable();
                    changedFields = false;
                    actions.get(ACTION_SAVE).setEnabled(false);
                    actions.get(ACTION_RESET).setEnabled(false);
                }
            }

        }

        protected class ButtonDeleteFileListener extends AbstractAction {

            JFileChooser fc;

            public ButtonDeleteFileListener() {
                putValue(Action.NAME, "Удалить файл документа");
                putValue(Action.SHORT_DESCRIPTION,
                        "Удалить файл документа из БД");
            }

            public void actionPerformed(ActionEvent arg0) {

            }

        }

        @SuppressWarnings("serial")
        protected class ButtonSaveFileListener extends AbstractAction {

            JFileChooser fc;
            Boolean replace;

            public ButtonSaveFileListener() {
                putValue(Action.NAME, "Загрузить файл документа");
                putValue(Action.SHORT_DESCRIPTION,
                        "Загрузить файл  документа в БД");
            }

            public void actionPerformed(ActionEvent arg0) {

            }

        }

        @SuppressWarnings("serial")
        protected class ButtonLoadFileListener extends AbstractAction {

            JFileChooser fc;

            public ButtonLoadFileListener() {
                putValue(Action.NAME, "Выгрузить файл документа");
                putValue(Action.SHORT_DESCRIPTION,
                        "Сохранить файл документа к себе на компьютер");
            }

            public void actionPerformed(ActionEvent arg0) {

            }

        }

        @SuppressWarnings("serial")
        protected class ButtonPublishListener extends AbstractAction {

            public ButtonPublishListener() {
                putValue(Action.NAME, "Опубликовать документ");
                putValue(Action.SHORT_DESCRIPTION,
                        "Опубликовать документ в общем дереве документов");
            }

            public void actionPerformed(ActionEvent arg0) {
                Integer answer = 1;
                if (changedFields) {
                    Object[] options = { "Да", "Нет" };
                    answer = JOptionPane
                            .showOptionDialog(
                                    documentViewer,
                                    "Документ был изменён. Cохранить перед публикацией изменения?",
                                    "Предупреждение",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.QUESTION_MESSAGE, null,
                                    options, options[1]);
                }
                if (answer == 0) {
                    saveDocument();
                }
                publishDocument();
                Object[] options = { "Окей" };
                JOptionPane.showOptionDialog(documentViewer,
                        "Документ был успешно опубликован", "Успех",
                        JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE,
                        null, options, options[0]);
                updateTable();
                anyChanges = true;
                documentViewer.dispose();
                closeChildWindow();
            }

        }

        protected class MyDocumentListener implements DocumentListener {
            public void insertUpdate(DocumentEvent e) {
                textsChanges();
            }

            public void removeUpdate(DocumentEvent e) {
                textsChanges();
            }

            public void changedUpdate(DocumentEvent e) {
                textsChanges();
            }

        }

    }

    protected class ButtonViewListener extends AbstractAction {

        public ButtonViewListener() {
            putValue(Action.NAME, "Обзор документа");
            putValue(Action.SHORT_DESCRIPTION, "Ознакомиться с черновиком");
        }

        public void actionPerformed(ActionEvent arg0) {

            if (tableDocuments.getSelectedRow() != -1) {
                if (documentViewer == null) {
                    currentDocId = (ObjectId) tableDocuments.getModel()
                            .getValueAt(tableDocuments.getSelectedRow(), 0);
                    documentViewer = new DocumentViewer();
                }
            }

        }

    }

    protected class ButtonRemoveListener extends AbstractAction {

        public ButtonRemoveListener() {
            putValue(Action.NAME, "Удалить");
            putValue(Action.SHORT_DESCRIPTION, "Удалить документ");
        }

        public void actionPerformed(ActionEvent arg0) {

            if (tableDocuments.getSelectedRow() != -1) {
                if (documentViewer == null) {
                    currentDocId = (ObjectId) tableDocuments.getModel()
                            .getValueAt(tableDocuments.getSelectedRow(), 0);
                } else {
                    documentViewer.dispose();
                    closeChildWindow();
                }
                deleteDocument(currentDocId);
                updateTable();
            }

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
    class DocumentsTableModel extends AbstractTableModel {

        private String[] columnNames = { "id", "Имя документа", "Имя Автора",
                "Папка" };

        public DocumentsTableModel() {
            data = new Object[][] { { "", "", "", "" } };
        }

        public void fillTable(ArrayList<DBObject> array) {
            Integer size = array.size();
            if (size == 0) {
                data = new Object[][] { { "", "", "", "" } };
                buttonView.setEnabled(false);
                buttonRemove.setEnabled(false);
                return;
            }
            buttonView.setEnabled(true);
            buttonRemove.setEnabled(true);
            data = new Object[size][4];
            int i = 0;
            DBObject currentDocument;
            while (i < size) {
                currentDocument = array.get(i);
                data[i][0] = currentDocument.get("_id");
                data[i][1] = currentDocument.get("name");
                data[i][2] = currentDocument.get("author");
                data[i][3] = currentDocument.get("parentName");
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

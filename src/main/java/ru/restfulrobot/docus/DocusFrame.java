package ru.restfulrobot.docus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.xml.bind.DatatypeConverter;

import ru.restfulrobot.docus.DocTree.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

public class DocusFrame extends JFrame {

    // Строки для удобного пользования классами обработчиков событий
    private static final String ACTION_ADD = "add";
    private static final String ACTION_DELETE = "delete";
    private static final String ACTION_RESET = "reset";
    private static final String ACTION_CANCEL = "cancel";
    private static final String ACTION_SAVE = "save";
    private static final String ACTION_LOADFILE = "loadFile";
    private static final String ACTION_SAVEFILE = "saveFile";
    private static final String ACTION_DELETEFILE = "deleteFile";
    private static final String ACTION_CUT = "cutDoc";
    private static final String ACTION_PASTLEAFTER = "pastleDocAfter";
    private static final String ACTION_PASTLEBEFORE = "pastleDocBefore";
    private static final String ACTION_INSERT = "insertIntoDoc";
    private static final String ACTION_FIND = "searchForm";
    private static final String ACTION_FINDRESULT = "findDocs";
    private static final String ACTION_CLEARRESULT = "clearSearchResult";
    private static final String ACTION_SAVETOXML = "saveToXml";
    private static final String ACTION_LOADFROMXML = "loadFromXml";
    private static final String ACTION_SAVEUSERSANDDEPARTMENTSTOXML = "saveUsersAndDepartmentsToXml";
    private static final String ACTION_LOADUSERSANDDEPARTMENTSFROMXML = "loadUsersAndDepartmentsFromXml";
    private static final String ACTION_ENTER = "enter";
    private static final String ACTION_OUT = "out";
    private static final String ACTION_REG = "reg";
    private static final String ACTION_OPENCOMMENTS = "openComments";
    private static final String ACTION_OPENCHANGES = "openChanges";
    private static final String ACTION_APPOINTDEPARTMENT = "appointDepartament";
    private static final String ACTION_CONTROLDEPARTMENT = "controlDepartament";
    private static final String ACTION_CONTROLUSER = "controlUsers";
    private static final String ACTION_CONTROLREVIEWERS = "controlReviewers";
    private static final String ACTION_CONTROLWORKERS = "controlWorkers";
    private static final String ACTION_CONTROLDOCUMENTS = "controlDocuments";


    public JTree jTree;
    private DocTree root;
    private static final long serialVersionUID = 2264040478371069163L;
    private int value = 0;
    private JLabel modified;
    private JLabel created;
    private JLabel labelDepartmentValue;
    private JLabel statusFile;
    private JTextField textName;
    private JTextField textAuthor;
    private JTextArea textTags;
    private JTextField textQuery;
    private Map<String, Action> actions = new HashMap<String, Action>();
    private DocusFrame mainwindow;
    private Boolean specialflag;
    private String bufferName;
    private String bufferAuthor;
    private String bufferTags;
    private String query;
    private String userName;
    private String department;
    private String[] departments;
    private StatusBar statusBar;
    private UserBar userBar;
    private TreePath bufferTrek;
    private Document[] resultsSeachDocumentsId;
    private Document bufferDoc;
    private Document parentNewDoc;
    private Document bufferselected;
    private Document cutDocument;
    private Document currentSelectedDocument;
    private Boolean chief;
    private Boolean changesApplied;
    private Boolean noChangeText;
    private Boolean returnBack;
    private Boolean readyToAdd;
    private Boolean cancelAdd;
    private Boolean cutDocFlag;
    private Boolean resultSearch;
    private JMenuBar mb;
    private JMenu mAdmin;
    private JMenuItem mcontrolUsers;
    private JMenuItem mcontrolReviewers;
    private JMenuItem mcontrolWorkers;
    private JMenuItem mcontrolDepartment;
    private JMenuItem mappointDepartment;
    private JMenuItem mcontrolDocuments;
    private MongoClient mongo;
    private DB db;
    private DBCollection table;
    private JTable tableResultsSearch;
    private MyRenderer renderer;
    private Integer heightWindow;
    private Dimension dimension;
    private JPanel mainPanel;
    private JPanel treePanel;

    // главная форма
    static private DocusFrame frame;
    static private DocusFrame searchFrame;
    static public DocusFrame parent;
    static public DocusAuthorizationForm loginFrame;
    static public DocusRegistrationForm registrationFrame;
    static public DocusControlUsersForm controlUserFrame;
    static public DocusAppointDepartmentForm appointDepartmentFrame;
    static public DocusControlDepartmentForm controlDepartmentFrame;
    static public DocusChiefForm chiefFrame;
    static public DocusReviewerForm reviewerFrame;
    static public DocusControlReviewers controlReviewerFrame;
    static public DocusComments commentsFrame;
    static public DocusChanges changesFrame;

    // вызов главной формы
    public static void main(String[] args)  {
        frame = new DocusFrame();
        frame.enterHowUnregistred();
        frame.init(args[0]);
        frame.setSize(884, 595);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    // инициирование главной формы
    public void init(String address) {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(value);
            }
        });
        mainwindow = this;
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.white);
        setTitle("Хранилище документов");
        this.setContentPane(mainPanel);
        mainPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        createActions();
        initConnection(address);
        resetFlags();
        initTree();
        treePanel = createTreePanel();
        mainPanel.add(treePanel, BorderLayout.WEST);
        mainPanel.add(createEditForm(), BorderLayout.CENTER);
        JPanel statusBarPanel = new JPanel(new BorderLayout());
        statusBarPanel.add(getStatusBar(), BorderLayout.NORTH);
        statusBarPanel.add(getUserBar(), BorderLayout.SOUTH);
        mainPanel.add(statusBarPanel, BorderLayout.SOUTH);
        this.setJMenuBar(initMenu());
        disableAllButtons();
        setButtonsCutPastleInsert(true, false, false);
        initFilterDocButtonsForUser();
        userBar.getUserInfo();
    }

    // авторизация пользователя
    public void authorization(String userRole, String userName,
                              String department, Boolean isChief, String[] departments) {
        this.userName = userName;
        this.department = department;
        this.chief = isChief;
        if (department.equals("reviewers")) {
            this.departments = departments;
        }
        closeAllChildsWindows();
        statusBar.setText("Добро пожаловать, " + userName + " !");
        userBar.getUserInfo();
        getAdminMenu();
        resetFlags();
        jTree.clearSelection();
        initFilterDocButtonsForUser();
        closeProhibitedFolders();
        TreePath trek = jTree.getPathForRow(0);
        jTree.setSelectionPath(trek);
    }

    // проверяем допуск к документу
    private void getAccessToTheDocument(Document doc) {
        AccesToFile accesToFile = null;
        switch (department) {
            case "general": {
                if (chief) {
                    textName.setEnabled(true);
                    textAuthor.setEnabled(true);
                    textTags.setEnabled(true);
                    accesToFile = AccesToFile.FULL;

                    if (doc.getParent() != null
                            && doc.getParent().equals(root.getMainNode())) {
                        mappointDepartment.setEnabled(true);
                    } else {
                        mappointDepartment.setEnabled(false);
                    }

                    if (cutDocFlag) {
                        setButtonsAddAndDelete(false, false);
                        actions.get(ACTION_INSERT).setEnabled(true);
                        actions.get(ACTION_CUT).setEnabled(false);
                        if (doc.equals(root.getMainNode())) {
                            actions.get(ACTION_PASTLEAFTER).setEnabled(false);
                            actions.get(ACTION_PASTLEBEFORE).setEnabled(false);
                            actions.get(ACTION_CANCEL).setEnabled(true);
                        } else {
                            actions.get(ACTION_PASTLEAFTER).setEnabled(true);
                            actions.get(ACTION_PASTLEAFTER).setEnabled(true);
                        }
                    } else {
                        if (doc.equals(root.getMainNode())) {
                            actions.get(ACTION_CUT).setEnabled(false);
                            setButtonsAddAndDelete(true, false);
                        } else {
                            actions.get(ACTION_CUT).setEnabled(true);
                        }
                    }
                    checkFile(doc, accesToFile);
                    return;
                } else {
                    accesToFile = AccesToFile.HIDDEN;
                    actions.get(ACTION_CUT).setEnabled(false);
                    actions.get(ACTION_ADD).setEnabled(false);
                    actions.get(ACTION_DELETE).setEnabled(false);
                    textName.setEnabled(false);
                    textAuthor.setEnabled(false);
                    textTags.setEnabled(false);
                }
            }

            break;

            default: {

                if (getAccesForThisDepartment(doc.getDepartment())
                        || doc.getDepartment().equals("general")) {

                    if (doc.getParent() != null
                            && doc.getParent().equals(root.getMainNode())) {

                        if (cutDocFlag) {
                            actions.get(ACTION_PASTLEBEFORE).setEnabled(false);
                            actions.get(ACTION_PASTLEAFTER).setEnabled(false);
                            actions.get(ACTION_INSERT).setEnabled(true);
                        } else {
                            actions.get(ACTION_CUT).setEnabled(false);
                            actions.get(ACTION_ADD).setEnabled(true);
                            actions.get(ACTION_DELETE).setEnabled(false);
                            actions.get(ACTION_PASTLEAFTER).setEnabled(false);
                            actions.get(ACTION_PASTLEBEFORE).setEnabled(false);
                        }
                        if (chief) {
                            accesToFile = AccesToFile.FULL;
                            textName.setEnabled(true);
                            textAuthor.setEnabled(true);
                            textTags.setEnabled(true);
                        } else {
                            accesToFile = AccesToFile.PARTIAL;
                            textName.setEnabled(false);
                            textAuthor.setEnabled(false);
                            textTags.setEnabled(false);
                        }
                    }

                    else {
                        if (cutDocFlag) {
                            setButtonsAddAndDelete(false, false);
                            actions.get(ACTION_CANCEL).setEnabled(true);
                            if (doc.equals(root.getMainNode())) {
                                actions.get(ACTION_PASTLEAFTER).setEnabled(false);
                                actions.get(ACTION_PASTLEBEFORE).setEnabled(false);
                                actions.get(ACTION_INSERT).setEnabled(false);
                            } else {
                                actions.get(ACTION_PASTLEAFTER).setEnabled(true);
                                actions.get(ACTION_PASTLEBEFORE).setEnabled(true);
                                actions.get(ACTION_INSERT).setEnabled(true);
                            }
                        } else {

                            if (doc.getParent() != null) {
                                accesToFile = AccesToFile.FULL;
                                actions.get(ACTION_CUT).setEnabled(true);
                                actions.get(ACTION_ADD).setEnabled(true);
                                actions.get(ACTION_DELETE).setEnabled(true);
                                textName.setEnabled(true);
                                textAuthor.setEnabled(true);
                                textTags.setEnabled(true);
                            } else {
                                accesToFile = AccesToFile.HIDDEN;
                                actions.get(ACTION_CUT).setEnabled(false);
                                actions.get(ACTION_ADD).setEnabled(false);
                                actions.get(ACTION_DELETE).setEnabled(false);
                            }

                        }

                    }

                } else {
                    accesToFile = AccesToFile.HIDDEN;
                    actions.get(ACTION_CUT).setEnabled(false);
                    actions.get(ACTION_ADD).setEnabled(false);
                    actions.get(ACTION_DELETE).setEnabled(false);
                    textName.setEnabled(false);
                    textAuthor.setEnabled(false);
                    textTags.setEnabled(false);

                }

            }
            break;
        }

        checkFile(doc, accesToFile);

    }

    // допуск к этому департменту
    private Boolean getAccesForThisDepartment(String department) {
        if (departments != null) {
            return checkDepartmentForReviewers(department);
        }
        return this.department.equals(department);
    }

    // перезагрузка дерева
    private void reload() {
        resetFlags();
        initTree();
        mainPanel.remove(treePanel);
        treePanel = createTreePanel();
        mainPanel.add(createTreePanel(), BorderLayout.WEST);
        mainPanel.repaint();
        mainwindow.revalidate();
        jTree.clearSelection();
        closeAllChildsWindows();

    }

    // закртие всех окон
    private void closeAllChildsWindows() {

        if (searchFrame != null) {
            searchFrame.dispose();
            this.searchFrame = null;
        }
        if (loginFrame != null) {
            loginFrame.dispose();
            this.loginFrame = null;
        }
        if (registrationFrame != null) {
            registrationFrame.dispose();
            this.registrationFrame = null;
        }
        if (controlUserFrame != null) {
            controlUserFrame.dispose();
            this.controlUserFrame = null;
        }
        if (controlDepartmentFrame != null) {
            controlDepartmentFrame.dispose();
            this.controlDepartmentFrame = null;
        }

        if (chiefFrame != null) {
            chiefFrame.dispose();
            this.chiefFrame = null;
        }

        if (commentsFrame != null) {
            commentsFrame.dispose();
            this.commentsFrame = null;
        }

        if (changesFrame != null) {
            changesFrame.dispose();
            this.changesFrame = null;
        }
    }

    // войти как незарегистрированный
    private void enterHowUnregistred() {
        userName = "unregistered";
        department = "general";
        chief = false;
    }

    // фильтр доступности кнопок для юзера
    private void initFilterDocButtonsForUser() {
        setButtonsAddAndDelete(false, false);
        actions.get(ACTION_CUT).setEnabled(false);
        actions.get(ACTION_PASTLEAFTER).setEnabled(false);
        actions.get(ACTION_PASTLEBEFORE).setEnabled(false);
        actions.get(ACTION_INSERT).setEnabled(false);

        if (userName.equals("unregistered")) {
            actions.get(ACTION_FIND).setEnabled(false);
            actions.get(ACTION_SAVETOXML).setEnabled(false);
            actions.get(ACTION_LOADFROMXML).setEnabled(false);
            actions.get(ACTION_SAVEUSERSANDDEPARTMENTSTOXML).setEnabled(false);
            actions.get(ACTION_LOADUSERSANDDEPARTMENTSFROMXML).setEnabled(false);
            return;
        }

        if (department.equals("general")) {

            if (chief) {
                actions.get(ACTION_FIND).setEnabled(true);
                actions.get(ACTION_SAVETOXML).setEnabled(true);
                actions.get(ACTION_LOADFROMXML).setEnabled(true);
                actions.get(ACTION_SAVEUSERSANDDEPARTMENTSTOXML).setEnabled(true);
                actions.get(ACTION_LOADUSERSANDDEPARTMENTSFROMXML).setEnabled(true);
                mappointDepartment.setEnabled(false);
            }

            else {
                actions.get(ACTION_FIND).setEnabled(true);
                actions.get(ACTION_SAVETOXML).setEnabled(false);
                actions.get(ACTION_LOADFROMXML).setEnabled(false);
            }

        } else {
            actions.get(ACTION_FIND).setEnabled(true);
            actions.get(ACTION_SAVETOXML).setEnabled(false);
            actions.get(ACTION_LOADFROMXML).setEnabled(false);
        }
    }

    // закрыть папки которые запрещены
    private void closeProhibitedFolders() {
        if (department.equals("general") && chief) {
            return;
        }
        Document[] childsOfRoot = root.getChilds();
        TreePath trek = jTree.getPathForRow(0);

        if (department.equals("reviewers")) {
            for (Document doc : childsOfRoot) {
                if (!checkDepartmentForReviewers(doc.getDepartment())
                        && !doc.getDepartment().equals("general")) {
                    trek = trek.pathByAddingChild(doc);
                    jTree.collapsePath(trek);
                    trek = jTree.getPathForRow(0);
                }
            }
            return;
        }
        for (Document doc : childsOfRoot) {
            if (!doc.getDepartment().equals(department)
                    && !doc.getDepartment().equals("general")) {
                trek = trek.pathByAddingChild(doc);
                jTree.collapsePath(trek);
                trek = jTree.getPathForRow(0);
            }
        }
    }

    // отключить кнопки управления документом
    private void disableAllButtons() {
        actions.get(ACTION_SAVE).setEnabled(false);
        actions.get(ACTION_RESET).setEnabled(false);
        actions.get(ACTION_CANCEL).setEnabled(false);
        actions.get(ACTION_LOADFILE).setEnabled(false);
        actions.get(ACTION_DELETEFILE).setEnabled(false);
        actions.get(ACTION_SAVEFILE).setEnabled(false);

    }

    // закрыть форму назначения департмента для текущего документа
    public void removeAppointDepartmentForm(Document currentDoc) {
        labelDepartmentValue.setText(currentDoc.getDepartment());
        appointDepartmentFrame = null;
    }

    // закрыть форму
    public void removeControlDepartmentForm() {
        controlDepartmentFrame = null;
    }

    // закрыть форму
    public void removeControlUserForm() {
        controlUserFrame = null;
    }

    // закрыть форму
    public void removeSearchForm() {
        searchFrame = null;
    }

    // закрыть форму
    public void removeLoginForm() {
        loginFrame = null;
    }

    // закрыть форму
    public void removeRegistrationForm() {
        registrationFrame = null;
    }

    // закрыть форму
    public void removeChiefForm() {
        chiefFrame = null;
    }

    // закрыть форму
    public void removeControlReviewerForm() {
        controlReviewerFrame = null;
    }

    // закрыть форму
    public void removeCommentsForm() {
        commentsFrame = null;
    }

    // закрыть форму
    public void removeChangesForm() {
        changesFrame = null;
    }

    // закрыть форму назначения модератора, если были изменения в дереве то удалить их
    public void removeReviewerForm(Boolean changesInTree) {
        if (changesInTree) {
            reload();
        }
        reviewerFrame = null;
    }

    // инициировать окно поиска
    private void inifSearchWindow(DocTree docTree, JTree jTreeFromMain,
                                  DocusFrame mainwindow, String department, Boolean chief) {
        this.frame = mainwindow;
        this.department = department;
        this.chief = chief;
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                clearSearchResults();
                frame.removeSearchForm();
            }
        });
        jTree = jTreeFromMain;
        root = docTree;
        createActionsForSearchWindow();
        DocusMediator.resetSearchResultArray();
        JPanel mainPanel = new JPanel(new BorderLayout());
        setTitle("Форма поиска");
        this.setContentPane(mainPanel);
        heightWindow = 440;
        this.setSize(650, heightWindow);
        this.setLocationRelativeTo(null);
        this.addComponentListener((ComponentListener) new WindowChangeSizeListener());
        mainPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        mainPanel.add(createSearchForm());
        JPanel statusBarPanel = new JPanel(new BorderLayout());
        statusBarPanel.setBackground(Color.white);
        statusBarPanel.add(getStatusBar());
        mainPanel.add(statusBarPanel);
        actions.get(ACTION_CLEARRESULT).setEnabled(false);
    }

    // содать форму поиска
    private Component createSearchForm() {
        resultSearch = false;
        JButton find = new JButton(actions.get(ACTION_FINDRESULT));
        JButton clear = new JButton(actions.get(ACTION_CLEARRESULT));
        JLabel labelQuery = new JLabel("ЗАПРОС:");
        JLabel labelResult = new JLabel("Результат запроса:");
        JPanel formPanel = new JPanel(new BorderLayout());
        JPanel form = new JPanel(new FlowLayout(FlowLayout.CENTER, 1500, 4));
        form.setPreferredSize(new Dimension(650, 1080));
        dimension = new Dimension(590, 170);
        textQuery = new JTextField(30);
        textQuery.setBorder(BorderFactory.createLineBorder(Color.black));
        tableResultsSearch = new JTable(new MyTableModel());
        tableResultsSearch.addMouseListener(new TableMouseListener());
        tableResultsSearch.setPreferredScrollableViewportSize(dimension);
        tableResultsSearch.setFillsViewportHeight(true);
        String[] items = { "Поиск по имени", "Поиск по автору",
                "Поиск по ключевым словам" };
        Container content = getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JComboBox<?> box = (JComboBox<?>) e.getSource();
                query = (String) box.getSelectedItem();
            }
        };
        JComboBox<?> comboBox = new JComboBox<Object>(items);
        comboBox.setAlignmentX(LEFT_ALIGNMENT);
        comboBox.addActionListener(actionListener);
        content.add(comboBox);
        JScrollPane scrollPane = new JScrollPane(tableResultsSearch);
        form.add(labelQuery);
        form.add(comboBox);
        form.add(textQuery);
        form.add(find);
        form.add(clear);
        form.add(labelResult);
        form.add(scrollPane);
        formPanel.add(form, BorderLayout.CENTER);
        return formPanel;
    }

    // инициирование соединения
    private void initConnection(String address) {
        try {
            mongo = new MongoClient(address, 27017);
            db = mongo.getDB("documents");
            table = db.getCollection("docs");

        } catch (Exception e) {
            statusBar.setMessage("Неизвестная ошибка");
        }

    }

    // иницирование деерва документов
    private void initTree() {
        root = new DocTree(table);
        jTree = new JTree(root.getMainNode());
        jTree.addTreeSelectionListener(new TreeListener());
        jTree.addTreeWillExpandListener(new TreeExpandListener());
        renderer = new MyRenderer();
        jTree.setCellRenderer(renderer);
    }

    // создание верхнего меню!
    private JMenuBar initMenu() {
        JMenuItem madd = new JMenuItem(actions.get(ACTION_ADD));
        JMenuItem mdel = new JMenuItem(actions.get(ACTION_DELETE));
        JMenuItem msav = new JMenuItem(actions.get(ACTION_SAVE));
        JMenuItem mres = new JMenuItem(actions.get(ACTION_RESET));
        JMenuItem mcan = new JMenuItem(actions.get(ACTION_CANCEL));
        JMenuItem mcut = new JMenuItem(actions.get(ACTION_CUT));
        JMenuItem mfind = new JMenuItem(actions.get(ACTION_FIND));
        JMenuItem minsert = new JMenuItem(actions.get(ACTION_INSERT));
        JMenuItem mpastleAfter = new JMenuItem(actions.get(ACTION_PASTLEAFTER));
        JMenuItem mpastleBefore = new JMenuItem(
                actions.get(ACTION_PASTLEBEFORE));
        JMenuItem menter = new JMenuItem(actions.get(ACTION_ENTER));
        JMenuItem mout = new JMenuItem(actions.get(ACTION_OUT));
        JMenuItem mreg = new JMenuItem(actions.get(ACTION_REG));
        JMenuItem mload = new JMenuItem(actions.get(ACTION_SAVETOXML));
        JMenuItem msave = new JMenuItem(actions.get(ACTION_LOADFROMXML));
        JMenuItem mloadUsDe = new JMenuItem(actions.get(ACTION_LOADUSERSANDDEPARTMENTSFROMXML));
        JMenuItem msaveUsDe = new JMenuItem(actions.get(ACTION_SAVEUSERSANDDEPARTMENTSTOXML));
        mcontrolUsers = new JMenuItem(actions.get(ACTION_CONTROLUSER));
        mcontrolDepartment = new JMenuItem(
                actions.get(ACTION_CONTROLDEPARTMENT));
        mappointDepartment = new JMenuItem(
                actions.get(ACTION_APPOINTDEPARTMENT));
        mcontrolWorkers = new JMenuItem(actions.get(ACTION_CONTROLWORKERS));
        mcontrolReviewers = new JMenuItem(actions.get(ACTION_CONTROLREVIEWERS));
        mcontrolDocuments = new JMenuItem(actions.get(ACTION_CONTROLDOCUMENTS));
        mb = new JMenuBar();
        mb.setVisible(true);
        JMenu mFile = new JMenu("Файл");
        mb.add(mFile);
        mAdmin = new JMenu("Админ Панель");
        JMenu mAut = new JMenu("Авторизация");
        mb.add(mAut);
        JMenu mMan = new JMenu("Управление документами");
        mb.add(mMan);
        JMenu mEdit = new JMenu("Редактирование");
        mb.add(mEdit);
        JMenu mInfo = new JMenu("Информация");
        mb.add(mInfo);
        JMenuItem minfoprog = new JMenuItem("О программе");
        minfoprog.addActionListener(new Info());
        mInfo.add(minfoprog);
        mInfo.addActionListener(new Info());
        mFile.add(mload);
        mFile.add(msave);
        mFile.add(msaveUsDe);
        mFile.add(mloadUsDe);
        mMan.add(madd);
        mMan.add(mdel);
        mMan.add(mcut);
        mMan.add(mpastleBefore);
        mMan.add(mpastleAfter);
        mMan.add(minsert);
        mEdit.add(msav);
        mEdit.add(mres);
        mEdit.add(mcan);
        mEdit.add(mfind);
        mAut.add(mreg);
        mAut.add(menter);
        mAut.add(mout);
        mAdmin.add(mcontrolUsers);
        mAdmin.add(mcontrolDepartment);
        mAdmin.add(mappointDepartment);
        mAdmin.add(mcontrolWorkers);
        mAdmin.add(mcontrolReviewers);
        mAdmin.add(mcontrolDocuments);
        return mb;
    }

    // добавление панели администратора
    private void getAdminMenu() {
        mb.add(mAdmin, 1);
        mAdmin.removeAll();
        if (chief) {
            if (department.equals("general")) {
                mAdmin.add(mcontrolDepartment);
                mAdmin.add(mcontrolUsers);
                mAdmin.add(mappointDepartment);
                mAdmin.add(mcontrolReviewers);
            } else {
                mAdmin.add(mcontrolWorkers);
            }
        } else {
            if (department.equals("reviewers")) {
                mAdmin.add(mcontrolDocuments);
                return;
            }
            if (mb.getComponentIndex(mAdmin) != -1) {
                mb.remove(mAdmin);
            }
        }
    }

    // создание формы редактирования документа
    private JPanel createEditForm() {
        JButton save = new JButton(actions.get(ACTION_SAVE));
        JButton cancel = new JButton(actions.get(ACTION_CANCEL));
        JButton reset = new JButton(actions.get(ACTION_RESET));
        JButton loadFile = new JButton(actions.get(ACTION_LOADFILE));
        JButton deleteFile = new JButton(actions.get(ACTION_DELETEFILE));
        JButton saveFile = new JButton(actions.get(ACTION_SAVEFILE));
        JButton openChanges = new JButton(actions.get(ACTION_OPENCHANGES));
        JButton openComments = new JButton(actions.get(ACTION_OPENCOMMENTS));
        JLabel label = new JLabel("Форма редактирования:");
        label.setHorizontalAlignment(0);
        JLabel labelName = new JLabel("Название документа:");
        JLabel labelAuthor = new JLabel("Автор:");
        JLabel labelTags = new JLabel("Ключевые слова:");
        JLabel labelCreated = new JLabel("Дата создания:");
        JLabel labelModified = new JLabel("Дата последнего изменения:");
        JLabel labelControl = new JLabel("Управление файлом");
        JLabel labelDepartment = new JLabel("Подразделение:");
        statusFile = new JLabel("-");
        created = new JLabel("-");
        modified = new JLabel("-");
        labelDepartmentValue = new JLabel("-");
        JPanel formPanel = new JPanel(new BorderLayout());
        JPanel form = new JPanel(new FlowLayout(FlowLayout.CENTER, 999, 4));
        form.setPreferredSize(new Dimension(300, 500));
        JPanel buttons = new JPanel(new GridLayout(0, 3, 0, 0));
        JPanel saveLoadFile = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JPanel commentsAndChanges = new JPanel(
                new FlowLayout(FlowLayout.CENTER));
        saveLoadFile.add(loadFile);
        saveLoadFile.add(deleteFile);
        saveLoadFile.add(saveFile);
        buttons.add(save);
        buttons.add(reset);
        buttons.add(cancel);
        commentsAndChanges.add(openChanges);
        commentsAndChanges.add(openComments);
        textName = new JTextField(30);
        textName.setBorder(BorderFactory.createLineBorder(Color.black));
        textName.setDisabledTextColor(Color.black);
        textName.getDocument().addDocumentListener(new MyDocumentListener());
        textAuthor = new JTextField(30);
        textAuthor.setBorder(BorderFactory.createLineBorder(Color.black));
        textAuthor.setDisabledTextColor(Color.black);
        textAuthor.getDocument().addDocumentListener(new MyDocumentListener());
        textTags = new JTextArea(5, 30);
        textTags.setWrapStyleWord(true);
        textTags.setLineWrap(true);
        textTags.setBorder(BorderFactory.createLineBorder(Color.black));
        textTags.setDisabledTextColor(Color.black);
        textTags.getDocument().addDocumentListener(new MyDocumentListener());
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
        form.add(commentsAndChanges);
        form.add(labelDepartment);
        form.add(labelDepartmentValue);
        formPanel.add(form, BorderLayout.CENTER);
        formPanel.add(buttons, BorderLayout.SOUTH);
        formPanel.add(label, BorderLayout.NORTH);

        actions.get(ACTION_OPENCOMMENTS).setEnabled(false);
        actions.get(ACTION_OPENCHANGES).setEnabled(false);

        return formPanel;
    }

    // создать панель дерева с возможность прокрутки и т д
    private JPanel createTreePanel() {
        JButton del = new JButton(actions.get(ACTION_DELETE));
        JButton add = new JButton(actions.get(ACTION_ADD));
        JPanel westPanel = new JPanel(new BorderLayout());
        westPanel.setPreferredSize(new Dimension(250, 300));
        JPanel buttons = new JPanel(new GridLayout(2, 0, 0, 0));
        JScrollPane scrollPane = new JScrollPane(jTree);
        westPanel.add(scrollPane, BorderLayout.CENTER);
        buttons.add(del);
        buttons.add(add);
        westPanel.add(buttons, BorderLayout.SOUTH);
        return westPanel;
    }

    // создание классов обработчиков событий и  укладка их в Map
    private void createActions() {
        actions.put(ACTION_DELETE, new ButtonDeleteListener());
        actions.put(ACTION_RESET, new ButtonResetListener());
        actions.put(ACTION_SAVE, new ButtonSaveListener());
        actions.put(ACTION_CANCEL, new ButtonCancelListener());
        actions.put(ACTION_ADD, new ButtonAddListener());
        actions.put(ACTION_SAVEFILE, new ButtonSaveFileListener());
        actions.put(ACTION_LOADFILE, new ButtonLoadFileListener());
        actions.put(ACTION_DELETEFILE, new ButtonDeleteFileListener());
        actions.put(ACTION_PASTLEAFTER, new ButtonPastleAfterListener());
        actions.put(ACTION_PASTLEBEFORE, new ButtonPastleBeforeListener());
        actions.put(ACTION_CUT, new ButtonCutListener());
        actions.put(ACTION_INSERT, new ButtonInsertListener());
        actions.put(ACTION_FIND, new ButtonSearchWindowListener());
        actions.put(ACTION_ENTER, new ButtonEnterListener());
        actions.put(ACTION_OUT, new ButtonOutListener());
        actions.put(ACTION_REG, new ButtonRegistrationListener());
        actions.put(ACTION_SAVETOXML, new ButtonSaveDataListener());
        actions.put(ACTION_LOADFROMXML, new ButtonLoadDataListener());
        actions.put(ACTION_APPOINTDEPARTMENT,
                new ButtonAppointDepartmentListener());
        actions.put(ACTION_CONTROLDEPARTMENT,
                new ButtonControlDepartmentListener());
        actions.put(ACTION_CONTROLUSER, new ButtonControlUsersListener());
        actions.put(ACTION_CONTROLREVIEWERS,
                new ButtonControlReviewersListener());
        actions.put(ACTION_CONTROLWORKERS, new ButtonControlWorkersListener());
        actions.put(ACTION_CONTROLDOCUMENTS,
                new ButtonControlDocumentsListener());
        actions.put(ACTION_OPENCOMMENTS, new ButtonOpenCommentsListener());
        actions.put(ACTION_OPENCHANGES, new ButtonOpenChangesListener());
        actions.put(ACTION_SAVEUSERSANDDEPARTMENTSTOXML, new ButtonSaveUsersAndDepartmentsToXmlListener());
        actions.put(ACTION_LOADUSERSANDDEPARTMENTSFROMXML, new ButtonLoadUsersAndDepartmentsFromXmlListener());
    }

    // создание классов обработчиков событий для окна поиска
    private void createActionsForSearchWindow() {
        actions.put(ACTION_FINDRESULT, new ButtonFindDocsListener());
        actions.put(ACTION_CLEARRESULT, new ButtonClearResultsListener());
    }

    // получить Статус бар
    private StatusBar getStatusBar() {
        if (statusBar == null) {
            statusBar = new StatusBar();
            statusBar.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 3));
            statusBar.setBorder(BorderFactory.createLineBorder(Color.black));
        }
        return statusBar;
    }

    // получить Юзер бар
    private UserBar getUserBar() {
        if (userBar == null) {
            userBar = new UserBar();
            userBar.setBorder(BorderFactory.createLineBorder(Color.black));
            userBar.setHorizontalAlignment(userBar.CENTER);
        }
        return userBar;
    }

    // проверка ана пустые поля
    private boolean emptyFields() {
        if (textName.getText().equals("") || textAuthor.getText().equals("")
                || textTags.getText().equals("")) {
            return true;
        }
        return false;
    }

    // проверка изменений текста
    private void textsChanges() {
        if (!noChangeText) {
            actions.get(ACTION_SAVE).setEnabled(true);
            actions.get(ACTION_RESET).setEnabled(true);
        }
        if (readyToAdd) {
            actions.get(ACTION_CANCEL).setEnabled(true);
        }
    }

    // управлени кнопками для файла в зависимости от доступа
    private void checkFile(Document doc, AccesToFile access) {
        actions.get(ACTION_SAVEFILE).setEnabled(false);
        actions.get(ACTION_DELETEFILE).setEnabled(false);
        actions.get(ACTION_LOADFILE).setEnabled(false);
        if (doc.getFile()) {
            switch (access) {
                case FULL:
                    actions.get(ACTION_DELETEFILE).setEnabled(true);
                    actions.get(ACTION_SAVEFILE).setEnabled(true);

                case PARTIAL:
                    actions.get(ACTION_LOADFILE).setEnabled(true);
                    break;
            }
            statusFile.setText(doc.getFileName());
        }

        else {
            if (access == AccesToFile.FULL) {
                actions.get(ACTION_SAVEFILE).setEnabled(true);

            }
            statusFile.setText("У данного документа отсутствует файл");
        }

    }

    // установить Едитэйбл для тектовых полей
    private void setTextFields(Boolean status) {
        textName.setEditable(status);
        textAuthor.setEditable(status);
        textTags.setEditable(status);

    }

    // выключить кнопки управления
    private void setControlsButtonsDisabled() {
        actions.get(ACTION_SAVE).setEnabled(false);
        actions.get(ACTION_RESET).setEnabled(false);
        actions.get(ACTION_CANCEL).setEnabled(false);
    }

    // изменить активность кнопок добавить и удалить
    private void setButtonsAddAndDelete(Boolean add, Boolean delete) {
        actions.get(ACTION_ADD).setEnabled(add);
        actions.get(ACTION_DELETE).setEnabled(delete);
    }

    // изменить активность кнопок вырезать, вставить после и до и вставить
    private void setButtonsCutPastleInsert(Boolean cut, Boolean pastle,
                                           Boolean insert) {

        actions.get(ACTION_CUT).setEnabled(cut);
        actions.get(ACTION_PASTLEBEFORE).setEnabled(pastle);
        actions.get(ACTION_PASTLEAFTER).setEnabled(pastle);
        actions.get(ACTION_INSERT).setEnabled(insert);

    }

    // сбросить флаги
    private void resetFlags() {
        noChangeText = true;
        returnBack = false;
        readyToAdd = false;
        cancelAdd = false;
        cutDocFlag = false;
        specialflag = true;
    }

    //сбросить результаты поиска перерисовать дерево
    private void clearSearchResults() {
        DocusMediator.resetSearchResultArray();
        jTree.treeDidChange();
    }

    // проверить департмент для модератора
    private Boolean checkDepartmentForReviewers(String docDepartment) {
        for (String currentDepartment : departments) {
            if (currentDepartment.equals(docDepartment)) {
                return true;
            }
        }
        return false;

    }

    // перечисления как доступ к файлу
    enum AccesToFile {
        HIDDEN, PARTIAL, FULL
    }

    // класс слушатель дерева. клик на элементе дерева
    protected class TreeListener implements TreeSelectionListener {
        Boolean selIsNull;

        public void valueChanged(TreeSelectionEvent e) {
            String addDocString = new String("Документ был успешно добавлен");
            selIsNull = false;
            Boolean comeout = false;
            noChangeText = true;
            setControlsButtonsDisabled();
            int n = 99;
            setButtonsAddAndDelete(true, true);
            Document sel = (Document) jTree.getLastSelectedPathComponent();
            if (sel == null) {
                selIsNull = true;
            }
            if (sel != null && sel.getParent() == null) {
                disableAllButtons();
                textName.setText(" ");
                textAuthor.setText(" ");
                modified.setText("-");
                created.setText("-");
                labelDepartmentValue.setText(sel.getDepartment());
                textTags.setText(" ");
                textName.setEnabled(false);
                textAuthor.setEnabled(false);
                textTags.setEnabled(false);
                getAccessToTheDocument(sel);
                actions.get(ACTION_OPENCOMMENTS).setEnabled(false);
                actions.get(ACTION_OPENCHANGES).setEnabled(false);
                actions.get(ACTION_SAVEFILE).setEnabled(false);
                specialflag = true;
                return;
            } else {
                actions.get(ACTION_OPENCOMMENTS).setEnabled(true);
                actions.get(ACTION_OPENCHANGES).setEnabled(true);
            }

            if (emptyFields() && !specialflag) {
                Object[] options = { "Окей" };

                JOptionPane
                        .showOptionDialog(
                                mainwindow,
                                "В предыдущем документе были оcтавлены пустые поля.\n Сохранение не будет произведено",
                                "Предупреждение", JOptionPane.OK_OPTION,
                                JOptionPane.ERROR_MESSAGE, null, options,
                                options[0]);
                if (readyToAdd) {
                    readyToAdd = false;
                }

            } else if (!readyToAdd && checkChanges() && !changesApplied
                    && !specialflag && !cancelAdd) {
                Object[] options = { "Да", "Нет", "Отмена" };
                n = JOptionPane
                        .showOptionDialog(mainwindow,
                                "Документ был изменён. Cохранить изменения?",
                                "Предупреждение",
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE, null, options,
                                options[2]);
            } else if (readyToAdd && !cancelAdd && !returnBack) {

                Object[] options = { "Да", "Нет", "Отмена" };
                n = JOptionPane
                        .showOptionDialog(
                                mainwindow,
                                "Была попытка добавить новый документ. Cохранить изменения?",
                                "Предупреждение",
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE, null, options,
                                options[2]);
            }

            switch (n) {
                case (0): {
                    if (readyToAdd) {
                        DefaultTreeModel model = (DefaultTreeModel) jTree
                                .getModel();
                        TreePath trek = jTree.getSelectionPath();
                        Document newdoc = root.createDocument(textName.getText(),
                                textAuthor.getText());
                        String tags = textTags.getText();
                        Scanner s = new Scanner(tags).useDelimiter(",\\s*");
                        while (s.hasNext()) {
                            newdoc.getTags().add(s.next());
                        }
                        s.close();
                        newdoc.setDepartmentLocal(parentNewDoc.getDepartment());
                        if (chief) {
                            parentNewDoc.insert(newdoc, -1);
                            newdoc.setDraft(false);

                        } else {
                            newdoc.setParent(parentNewDoc);
                            newdoc.setDraft(true);
                            Object[] options = { "Окей" };
                            JOptionPane
                                    .showOptionDialog(
                                            mainwindow,
                                            "Документ \""
                                                    + textName.getText()
                                                    + "\" будет проверен модератором перед добавлением",
                                            "Информация!", JOptionPane.OK_OPTION,
                                            JOptionPane.INFORMATION_MESSAGE, null,
                                            options, options[0]);

                        }
                        newdoc.setFile(false);
                        newdoc.firstChange(userName, department);
                        readyToAdd = false;
                        specialflag = true;
                        comeout = true;
                        setButtonsAddAndDelete(true, true);
                        setControlsButtonsDisabled();
                        model.reload();
                        jTree.setSelectionPath(trek);
                        statusBar.setText(addDocString);
                    } else {
                        bufferDoc.setName(bufferName);
                        bufferDoc.setAuthor(bufferAuthor);
                        bufferDoc.setModified(new Date());
                        String tags = bufferTags;
                        bufferDoc.getTags().clear();
                        Scanner s = new Scanner(tags).useDelimiter(",\\s*");
                        while (s.hasNext()) {
                            bufferDoc.getTags().add(s.next());
                        }
                        s.close();
                        bufferDoc.update();
                        DefaultTreeModel model = (DefaultTreeModel) jTree
                                .getModel();
                        TreePath trek = jTree.getSelectionPath();
                        changesApplied = true;
                        String str = new String("Был изменён документ \""
                                + bufferDoc.getName() + "\", написанный "
                                + bufferDoc.getAuthor());
                        statusBar.setMessage(str);
                        model.reload();
                        jTree.setSelectionPath(trek);
                        comeout = true;
                    }
                }
                break;
                case (1): {
                    if (readyToAdd) {
                        readyToAdd = false;
                        setButtonsAddAndDelete(true, true);
                    }

                }
                break;

                case (2): {
                    returnBack = true;
                    comeout = true;
                    jTree.setSelectionPath(bufferTrek);

                }
                break;
                default:
                    ;
                    break;

            }
            if (comeout) {
                return;
            }

            if (!selIsNull) {
                MutableTreeNode parent = (MutableTreeNode) sel.getParent();
                if (parent != null) {
                    specialflag = false;
                    Document doc = (Document) sel;
                    String tags = "";
                    if (returnBack) {

                        if (readyToAdd) {
                            actions.get(ACTION_CANCEL).setEnabled(true);
                            actions.get(ACTION_SAVE).setEnabled(true);
                            actions.get(ACTION_RESET).setEnabled(true);
                        } else {
                            noChangeText = false;
                            textsChanges();
                            textName.setText(bufferName);
                            textAuthor.setText(bufferAuthor);
                            textTags.setText(bufferTags);
                        }

                        returnBack = false;
                    } else {
                        getAccessToTheDocument(doc);
                        currentSelectedDocument = doc;
                        textName.setText(doc.getName());
                        textAuthor.setText(doc.getAuthor());
                        Date bufferDate;
                        bufferDate = doc.getModified();
                        DateFormat df = new SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss aa");
                        String dateStr = df.format(bufferDate);
                        created.setText(dateStr);
                        bufferDate = doc.getCreated();
                        dateStr = df.format(bufferDate);
                        modified.setText(dateStr);
                        labelDepartmentValue.setText(doc.getDepartment());
                        for (int i = 0; i < doc.getTags().size(); i++) {
                            tags += doc.getTags().get(i);
                            if (i + 1 != doc.getTags().size())
                                tags += ", ";
                        }
                        textTags.setText(tags);

                        if (cutDocFlag) {
                            setButtonsAddAndDelete(false, false);
                            disableAllButtons();
                            actions.get(ACTION_CANCEL).setEnabled(true);
                            setTextFields(false);
                        }
                    }
                    bufferName = doc.getName();
                    bufferAuthor = doc.getAuthor();
                    bufferTags = tags;
                    bufferDoc = doc;
                    bufferselected = sel;
                    bufferTrek = jTree.getSelectionPath();
                    noChangeText = false;
                    changesApplied = false;
                    cancelAdd = false;
                } else {
                    setControlsButtonsDisabled();
                    actions.get(ACTION_CANCEL).setEnabled(true);
                    actions.get(ACTION_SAVE).setEnabled(true);
                    actions.get(ACTION_RESET).setEnabled(true);
                    returnBack = false;
                    noChangeText = true;
                    textName.setText("");
                    textAuthor.setText("");
                    textTags.setText("");
                    modified.setText("-");
                    created.setText("-");
                    labelDepartmentValue.setText(sel.getDepartment());
                    specialflag = true;
                    cancelAdd = false;
                }
            }

        }

        // сохранение изменений если поля были изменены
        private boolean checkChanges() {
            if (bufferselected == null) {
                return false;
            }
            if (bufferName.equals(textName.getText())
                    && bufferAuthor.equals(textAuthor.getText())
                    && bufferTags.equals(textTags.getText())) {
                return false;
            }
            bufferName = textName.getText();
            bufferAuthor = textAuthor.getText();
            bufferTags = textTags.getText();
            return true;
        }
    }

    // класс прослушивает текстовые поля на изменение значений
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

    // слушатель кнопки Reset
    protected class ButtonResetListener extends AbstractAction {

        public ButtonResetListener() {
            putValue(Action.NAME, "Сбросить");
            putValue(Action.SHORT_DESCRIPTION, "Сброс изменений");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            if (readyToAdd) {
                textName.setText("Имя документа");
                textAuthor.setText("Автор документа");
                textTags.setText("Тэг1, Тэг2");
                setControlsButtonsDisabled();
                actions.get(ACTION_CANCEL).setEnabled(true);
                readyToAdd = true;

            } else {

                Document doc = (Document) jTree.getLastSelectedPathComponent();
                textName.setText(doc.getName());
                textAuthor.setText(doc.getAuthor());
                String tags = "";
                for (int i = 0; i < doc.getTags().size(); i++) {
                    tags += doc.getTags().get(i);
                    if (i + 1 != doc.getTags().size())
                        tags += ", ";
                }
                textTags.setText(tags);
                setControlsButtonsDisabled();
            }

            String str = new String("Сброс выполнен");
            statusBar.setMessage(str);
            if (!readyToAdd)
                specialflag = true;

        }
    }

    // слушатель кнопки Cut
    protected class ButtonCutListener extends AbstractAction {

        public ButtonCutListener() {
            putValue(Action.NAME, "Вырезать");
            putValue(Action.SHORT_DESCRIPTION,
                    "Вырезать документ из данной папки");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            cutDocument = (Document) jTree.getLastSelectedPathComponent();
            if (cutDocument == null || cutDocument.getParent() == null) {
                statusBar.setText("Нечего вырезать");
                return;

            }
            setButtonsCutPastleInsert(false, true, true);
            cutDocFlag = true;
            setButtonsAddAndDelete(false, false);
            disableAllButtons();

        }
    }

    // слушатель кнопки вставить после
    protected class ButtonPastleAfterListener extends AbstractAction {

        public ButtonPastleAfterListener() {
            putValue(Action.NAME, "Вставить после");
            putValue(Action.SHORT_DESCRIPTION,
                    "Вставить документ после выбранного");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            setButtonsCutPastleInsert(true, false, false);
            Document destinationDoc = (Document) jTree
                    .getLastSelectedPathComponent();
            if (destinationDoc.equals(cutDocument)
                    || destinationDoc.getParent() == null) {
                statusBar.setText("Ошибка вставки документа");
                cutDocFlag = false;
                cutDocument = null;
                setTextFields(true);
                setButtonsAddAndDelete(true, true);
                actions.get(ACTION_CANCEL).setEnabled(false);
                return;
            }
            Integer positionDestinationDoc = destinationDoc.getPosition();
            Integer positionSourceDoc = cutDocument.getPosition();
            Document parentCutDocument = cutDocument.getParent();
            if (parentCutDocument == destinationDoc.getParent()) {
                parentCutDocument.insertAfterInGeneralParent(cutDocument,
                        positionDestinationDoc, positionSourceDoc);
            } else {
                // вставка в childs нового parent
                destinationDoc.getParent().insert(cutDocument,
                        positionDestinationDoc + 1);
                // реордер у старого parent начиная с позиции предыдущего
                root.reOrder(null, parentCutDocument, positionSourceDoc - 1);
                // установить новую позицию
                cutDocument.setPosition(positionDestinationDoc + 1);
                cutDocument.setDepartmentGlobal(cutDocument.getParent()
                        .getDepartment());
                cutDocument.setDraft(false);
                // реордер у нового parent начиная с места вставки
                root.reOrder(destinationDoc, null, positionDestinationDoc);
            }
            DefaultTreeModel model = (DefaultTreeModel) jTree.getModel();
            TreePath path = jTree.getSelectionPath().getParentPath();
            cutDocFlag = false;
            setTextFields(true);
            setButtonsAddAndDelete(true, true);
            model.reload();
            jTree.setSelectionPath(path.pathByAddingChild(cutDocument));
            cutDocument = null;
        }

    }

    // слушатель кнопки вставить до
    protected class ButtonPastleBeforeListener extends AbstractAction {

        public ButtonPastleBeforeListener() {
            putValue(Action.NAME, "Вставить до");
            putValue(Action.SHORT_DESCRIPTION,
                    "Вставить документ до выбранного");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            setButtonsCutPastleInsert(true, false, false);
            Document destinationDoc = (Document) jTree
                    .getLastSelectedPathComponent();
            if (destinationDoc.equals(cutDocument)
                    || destinationDoc.getParent() == null) {
                statusBar.setText("Ошибка вставки документа");
                cutDocFlag = false;
                cutDocument = null;
                setTextFields(true);
                setButtonsAddAndDelete(true, true);
                actions.get(ACTION_CANCEL).setEnabled(false);
                return;
            }
            Integer positionDestinationDoc = destinationDoc.getPosition();
            Integer positionSourceDoc = cutDocument.getPosition();
            Document parentCutDocument = cutDocument.getParent();
            if (parentCutDocument == destinationDoc.getParent()) {

                parentCutDocument.insertBeforeInGeneralParent(cutDocument,
                        positionDestinationDoc, positionSourceDoc);
            } else {
                // вставка в childs нового parent
                destinationDoc.getParent().insert(cutDocument,
                        positionDestinationDoc);
                // реордер у старого parent начиная с позиции предыдущего
                root.reOrder(null, parentCutDocument, positionSourceDoc - 1);
                // установить новую позицию
                cutDocument.setPosition(positionDestinationDoc);
                cutDocument.setDepartmentGlobal(cutDocument.getParent()
                        .getDepartment());
                cutDocument.setDraft(false);
                // реордер у нового parent начиная с места вставки
                root.reOrder(destinationDoc, null, positionDestinationDoc);
            }
            DefaultTreeModel model = (DefaultTreeModel) jTree.getModel();
            TreePath path = jTree.getSelectionPath().getParentPath();
            cutDocFlag = false;
            setTextFields(true);
            setButtonsAddAndDelete(true, true);
            model.reload();
            jTree.setSelectionPath(path.pathByAddingChild(cutDocument));
            cutDocument = null;

        }

    }

    // слушатель кнопки  вложить
    protected class ButtonInsertListener extends AbstractAction {

        public ButtonInsertListener() {
            putValue(Action.NAME, "Вложить");
            putValue(Action.SHORT_DESCRIPTION,
                    "Вложить внутрь данного документа");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            setButtonsCutPastleInsert(true, false, false);
            Document sel = (Document) jTree.getLastSelectedPathComponent();

            if (sel.equals(cutDocument) || sel.checkAncstors(cutDocument)) {

                statusBar.setText("Ошибка вложения документа");
                cutDocFlag = false;
                cutDocument = null;
                setTextFields(true);
                setButtonsAddAndDelete(true, true);
                actions.get(ACTION_CANCEL).setEnabled(false);
                return;
            }
            Integer positionSelectedDoc = cutDocument.getPosition();
            Document parentCutDocument = cutDocument.getParent();
            sel.insert(cutDocument, -1);
            root.reOrder(null, parentCutDocument, positionSelectedDoc - 1);
            cutDocument.setPosition(sel.getChildCount() - 1);
            cutDocument.setDepartmentGlobal(sel.getDepartment());
            cutDocument.setDraft(false);
            DefaultTreeModel model = (DefaultTreeModel) jTree.getModel();
            cutDocFlag = false;
            setTextFields(true);
            setButtonsAddAndDelete(true, true);
            TreePath path = jTree.getSelectionPath();
            model.reload();
            jTree.setSelectionPath(path.pathByAddingChild(cutDocument));
            cutDocument = null;

        }

    }

    // слушатель кнопки удалить
    protected class ButtonDeleteListener extends AbstractAction {
        public ButtonDeleteListener() {
            putValue(Action.NAME, "Удалить");
            putValue(Action.SHORT_DESCRIPTION, "Удалить документ");
        }

        public void actionPerformed(ActionEvent arg0) {

            DefaultTreeModel model = (DefaultTreeModel) jTree.getModel();
            Document doc = (Document) jTree.getLastSelectedPathComponent();
            if (doc != null) {
                MutableTreeNode parent = (MutableTreeNode) doc.getParent();

                if (parent != null) {

                    Object[] options = { "Ок", "Отмена" };
                    int n;
                    Boolean childs = !(doc.getChildCount() == 0);
                    if (!childs) {
                        n = JOptionPane
                                .showOptionDialog(
                                        mainwindow,
                                        "Вы действительно хотите удалить данный документ?",
                                        "Предупреждение",
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.QUESTION_MESSAGE, null,
                                        options, options[1]);
                    } else {
                        n = JOptionPane
                                .showOptionDialog(
                                        mainwindow,
                                        "Вы действительно хотите удалить данный документ и всех его потомков?",
                                        "Предупреждение",
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.QUESTION_MESSAGE, null,
                                        options, options[1]);

                    }
                    if (n == 0) {

                        Document parentDeletedDoc = doc.getParent();
                        Integer positionSelectedDoc = doc.getPosition();
                        if (doc.getFile()) {
                            try {
                                GridFS gfsDoc = new GridFS(db, "DocFiles");
                                Document sel = (Document) jTree
                                        .getLastSelectedPathComponent();
                                gfsDoc.remove(gfsDoc.findOne(sel.getId()));
                            } catch (MongoException e) {
                                e.printStackTrace();
                            }
                        }
                        doc.removeFromParent();

                        root.reOrder(null, parentDeletedDoc,
                                positionSelectedDoc - 1);
                        model.reload();
                        String str = new String("Был удалён документ \""
                                + doc.getName() + "\", написанный "
                                + doc.getAuthor());

                        if (childs) {
                            str += " и всё что с ним связано тоже";
                        }
                        statusBar.setMessage(str);
                        specialflag = true;
                    }

                } else {
                    statusBar.setMessage("Нечего удалять :-(");

                }
            } else
                extracted();

        }

        private void extracted() {
            {
                statusBar.setMessage("Выберите документ для удаления");
            }
        }
    }

    // слушатель кнопки сохранить
    protected class ButtonSaveListener extends AbstractAction {

        public ButtonSaveListener() {
            putValue(Action.NAME, "Сохранить");
            putValue(Action.SHORT_DESCRIPTION, "Сохранить изменения");
        }

        public void actionPerformed(ActionEvent arg0) {

            DefaultTreeModel model = (DefaultTreeModel) jTree.getModel();
            TreePath trek = jTree.getSelectionPath();
            Document sel = (Document) jTree.getLastSelectedPathComponent();

            if (sel != null) {

                if (emptyFields()) {
                    Object[] options = { "Ок" };

                    JOptionPane.showOptionDialog(mainwindow,
                            "Заполните, пожалуйста, пустые поля",
                            "Предупреждение", JOptionPane.OK_OPTION,
                            JOptionPane.ERROR_MESSAGE, null, options,
                            options[0]);
                    return;

                }
                String changeDocString = new String(
                        "Документ был успешно изменён");
                String addDocString = new String(
                        "Документ был успешно добавлен");
                Document doc = sel;

                if (readyToAdd) {

                    Document newdoc = root.createDocument(textName.getText(),
                            textAuthor.getText());
                    String tags = textTags.getText();
                    Scanner s = new Scanner(tags).useDelimiter(",\\s*");
                    while (s.hasNext()) {
                        newdoc.getTags().add(s.next());
                    }
                    s.close();
                    newdoc.setDepartmentLocal(parentNewDoc.getDepartment());
                    TreePath path = jTree.getSelectionPath();
                    if (chief) {
                        parentNewDoc.insert(newdoc, -1);
                        newdoc.setDraft(false);
                        path = path.pathByAddingChild(newdoc);

                    } else {
                        newdoc.setParent(parentNewDoc);
                        newdoc.setDraft(true);
                        Object[] options = { "Окей" };
                        JOptionPane
                                .showOptionDialog(
                                        mainwindow,
                                        "Документ \""
                                                + textName.getText()
                                                + "\" будет проверен модератором перед добавлением",
                                        "Информация!", JOptionPane.OK_OPTION,
                                        JOptionPane.INFORMATION_MESSAGE, null,
                                        options, options[0]);

                    }
                    newdoc.setFile(false);
                    newdoc.firstChange(userName, department);
                    specialflag = true;
                    readyToAdd = false;
                    setButtonsAddAndDelete(true, true);
                    changesApplied = true;
                    setControlsButtonsDisabled();
                    model.reload();
                    jTree.setSelectionPath(path);
                    statusBar.setMessage(addDocString);

                } else {
                    String tags = textTags.getText();
                    doc.getTags().clear();
                    Scanner s = new Scanner(tags).useDelimiter(",\\s*");
                    while (s.hasNext()) {
                        doc.getTags().add(s.next());
                    }
                    s.close();
                    doc.setName(textName.getText());
                    doc.setAuthor(textAuthor.getText());
                    Date change = new Date();
                    doc.setModified(change);
                    doc.appendChange(userName, department, false, change);
                    doc.update();
                    changesApplied = true;
                    statusBar.setMessage(changeDocString);
                    model.reload();
                    jTree.setSelectionPath(trek);
                    setControlsButtonsDisabled();
                }

            }

        }

    }

    // слушатель кнопки отмена
    protected class ButtonCancelListener extends AbstractAction {

        public ButtonCancelListener() {
            putValue(Action.NAME, "Отмена");
            putValue(Action.SHORT_DESCRIPTION, "Отмена добавления");
        }

        public void actionPerformed(ActionEvent arg0) {

            DefaultTreeModel model = (DefaultTreeModel) jTree.getModel();
            if (cutDocFlag) {
                actions.get(ACTION_CANCEL).setEnabled(false);
                setButtonsAddAndDelete(true, true);
                cutDocFlag = false;
                cutDocument = null;
                setTextFields(true);
                setButtonsCutPastleInsert(true, false, false);
                model.reload();
                return;

            }

            if (readyToAdd) {

				/*
				 * readyToAdd = false; cancelAdd = true;
				 *
				 * model.reload(); jTree.setSelectionPath(bufferTrek);
				 * setButtonsAddAndDelete(true, true);
				 */

                readyToAdd = false;
                cancelAdd = true;

                Document sel = (Document) bufferTrek.getLastPathComponent();

                model.reload();
                jTree.setSelectionPath(bufferTrek);
                setButtonsAddAndDelete(true, (sel.getParent() != null));

            }
            String str = new String("Отмена добавления");
            statusBar.setMessage(str);
        }
    }

    // слушатель кнопки добавить
    protected class ButtonAddListener extends AbstractAction {

        public ButtonAddListener() {
            putValue(Action.NAME, "Добавить");
            putValue(Action.SHORT_DESCRIPTION, "Добавить документ");
        }

        Document doc;

        public void actionPerformed(ActionEvent arg0) {
            textName.setEnabled(true);
            textAuthor.setEnabled(true);
            textTags.setEnabled(true);

            Document sel = (Document) jTree.getLastSelectedPathComponent();
            if (sel == null) {
                statusBar.setMessage("Некуда добавлять документы");

                return;
            }
            statusBar.setMessage("Введите данные для нового документа");
            setButtonsAddAndDelete(true, true);
            textName.setEditable(true);
            textAuthor.setEditable(true);
            textTags.setEnabled(true);
            Boolean comeout = false;
            DefaultTreeModel model = (DefaultTreeModel) jTree.getModel();
            TreePath trek = jTree.getSelectionPath();

            parentNewDoc = sel;
            if (sel.getParent() != null) {
                doc = sel;

                if (emptyFields() && !specialflag) {
                    Object[] options = { "Окей" };
                    JOptionPane
                            .showOptionDialog(
                                    mainwindow,
                                    "В предыдущем документе были оcтавлены пустые поля.\n Сохранение не будет произведено",
                                    "Предупреждение", JOptionPane.OK_OPTION,
                                    JOptionPane.ERROR_MESSAGE, null, options,
                                    options[0]);
                } else if (checkChanges() && !changesApplied && !specialflag
                        && !cancelAdd) {
                    Object[] options = { "Да", "Нет", "Отмена" };
                    int n = JOptionPane.showOptionDialog(mainwindow,
                            "Документ был изменён. Cохранить изменения?",
                            "Предупреждение", JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE, null, options,
                            options[2]);
                    switch (n) {
                        case (0): {

                            bufferDoc.setName(textName.getText());
                            bufferDoc.setAuthor(textAuthor.getText());
                            String tags = textTags.getText();
                            bufferDoc.getTags().clear();
                            Scanner s = new Scanner(tags).useDelimiter(",\\s*");
                            while (s.hasNext()) {
                                bufferDoc.getTags().add(s.next());
                            }
                            s.close();
                            changesApplied = true;
                            String str = new String("Был изменён документ \""
                                    + bufferDoc.getName() + "\", написанный "
                                    + bufferDoc.getAuthor());
                            statusBar.setMessage(str);

                            model.reload();
                            jTree.setSelectionPath(trek);

                        }
                        break;

                        case (2): {

                            returnBack = true;
                            comeout = true;
                            setButtonsAddAndDelete(true, true);
                            jTree.setSelectionPath(bufferTrek);

                        }
                        break;
                        default:
                            ;
                            break;
                    }
                }

            }

            if (comeout) {
                return;
            }

            if (sel != null) {
                MutableTreeNode parent = (MutableTreeNode) sel.getParent();
                textName.setText("Имя документа");
                textAuthor.setText("Автор документа");
                textTags.setText("Тэг1, Тэг2");
                readyToAdd = true;
                bufferTrek = trek;
                if (parent == null) {
                    setButtonsAddAndDelete(false, true);
                    noChangeText = false;
                    specialflag = false;
                } else {
                    disableAllButtons();
                    actions.get(ACTION_CANCEL).setEnabled(true);
                }

            }
        }

        private boolean checkChanges() {
            actions.get(ACTION_ADD).setEnabled(false);
            String tags = "";
            for (int i = 0; i < doc.getTags().size(); i++) {
                tags += doc.getTags().get(i);
                if (i + 1 != doc.getTags().size())
                    tags += ", ";

            }
            if (doc.getName().equals(textName.getText())
                    && doc.getAuthor().equals(textAuthor.getText())
                    && tags.equals(textTags.getText())) {
                return false;
            }
            return true;
        }

    }

    // слушатель кнопки удалить файл
    protected class ButtonDeleteFileListener extends AbstractAction {

        JFileChooser fc;

        public ButtonDeleteFileListener() {
            putValue(Action.NAME, "Удалить файл документа");
            putValue(Action.SHORT_DESCRIPTION, "Удалить файл документа из БД");
        }

        public void actionPerformed(ActionEvent arg0) {

            Object[] options = { "Ок", "Отмена" };
            int n;
            n = JOptionPane
                    .showOptionDialog(
                            mainwindow,
                            "Вы действительно хотите удалить файл данного документа из БД?",
                            "Предупреждение", JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE, null, options,
                            options[1]);

            if (n == 0) {
                try {
                    GridFS gfsDoc = new GridFS(db, "DocFiles");
                    Document sel = (Document) jTree
                            .getLastSelectedPathComponent();

                    gfsDoc.remove(gfsDoc.findOne(sel.getId()));
                    sel.setFile(false);
                    checkFile(sel, AccesToFile.FULL);
                    statusBar.setText("Файл документа успешно удалён из БД");
                } catch (MongoException e) {
                    e.printStackTrace();
                }
            }

        }

    }

    // слушатель кнопки загрузить файл
    protected class ButtonSaveFileListener extends AbstractAction {

        JFileChooser fc;
        Boolean replace;

        public ButtonSaveFileListener() {
            putValue(Action.NAME, "Загрузить файл документа");
            putValue(Action.SHORT_DESCRIPTION, "Загрузить файл  документа в БД");
        }

        public void actionPerformed(ActionEvent arg0) {
            replace = false;
            Document sel = (Document) jTree.getLastSelectedPathComponent();
            if (sel.getFile()) {

                Object[] options = { "Ок", "Отмена" };
                int n;
                n = JOptionPane
                        .showOptionDialog(
                                mainwindow,
                                "У даннаго документа уже есть файл. Хотите заменить его новым?",
                                "Предупреждение", JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE, null, options,
                                options[1]);

                if (n == 0) {
                    replace = true;
                } else {
                    return;

                }

            }

            fc = new JFileChooser();
            int returnVal = fc.showOpenDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                try {
                    GridFS gfsDoc = new GridFS(db, "DocFiles");
                    if (replace) {
                        gfsDoc.remove(gfsDoc.findOne(sel.getId()));
                        sel.setFile(false);

                    }
                    String newFileName = file.getName();
                    File docFile = new File(file.getAbsolutePath());
                    GridFSInputFile gfsFile = gfsDoc.createFile(docFile);
                    gfsFile.setFilename(newFileName);
                    gfsFile.setId(sel.getId());
                    gfsFile.save();
                    sel.setFile(true, newFileName);
                    checkFile(sel, AccesToFile.FULL);
                    statusBar
                            .setText("Файл документа был успешно загружен в БД");

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (MongoException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    // слушатель кнопки сохранить файл к себе на компьютер
    protected class ButtonLoadFileListener extends AbstractAction {

        JFileChooser fc;

        public ButtonLoadFileListener() {
            putValue(Action.NAME, "Выгрузить файл документа");
            putValue(Action.SHORT_DESCRIPTION,
                    "Сохранить файл документа к себе на компьютер");
        }

        public void actionPerformed(ActionEvent arg0) {

            fc = new JFileChooser();
            try {
                GridFS gfsDoc = new GridFS(db, "DocFiles");
                Document sel = (Document) jTree.getLastSelectedPathComponent();
                GridFSDBFile fileForOutput = gfsDoc.findOne(sel.getId());
                fc.setSelectedFile(new File(fileForOutput.getFilename()));
                int returnVal = fc.showSaveDialog(frame);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    fileForOutput.writeTo(file.getAbsolutePath());
                }

            }

            catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (MongoException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    // слушатель кнопки поиск
    protected class ButtonSearchWindowListener extends AbstractAction {

        public ButtonSearchWindowListener() {
            putValue(Action.NAME, "Поиск");
            putValue(Action.SHORT_DESCRIPTION,
                    "Поиск документов по определенным параметрам");
        }

        public void actionPerformed(ActionEvent arg0) {

            if (searchFrame == null) {
                searchFrame = new DocusFrame();
                searchFrame.inifSearchWindow(root, jTree, frame, department,
                        chief);
                searchFrame.setVisible(true);
            }

        }

    }

    // слушатель кнопки найти документы
    protected class ButtonFindDocsListener extends AbstractAction {

        public ButtonFindDocsListener() {
            putValue(Action.NAME, "Найти");
            putValue(Action.SHORT_DESCRIPTION, "Найти документы");
        }

        public void actionPerformed(ActionEvent arg0) {

            ArrayList<Document> docs;
            if (query == null) {
                query = "Поиск по имени";
            }
            switch (query) {

                case "Поиск по имени":
                    if (department.equals("general") && chief) {
                        docs = (ArrayList<Document>) root.findByName(textQuery
                                .getText());
                    } else {
                        docs = (ArrayList<Document>) root.findByName(
                                textQuery.getText(), department, departments);
                    }
                    break;

                case "Поиск по автору":
                    if (department.equals("general") && chief) {
                        docs = (ArrayList<Document>) root.findByAuthor(textQuery
                                .getText());
                    } else {
                        docs = (ArrayList<Document>) root.findByAuthor(
                                textQuery.getText(), department, departments);
                    }
                    break;

                case "Поиск по ключевым словам":
                    if (department.equals("general") && chief) {
                        docs = (ArrayList<Document>) root.findTags(textQuery
                                .getText());
                    } else {
                        docs = (ArrayList<Document>) root.findTags(
                                textQuery.getText(), department, departments);
                    }
                    break;

                default:
                    if (department.equals("general")) {
                        docs = (ArrayList<Document>) root.findByName(textQuery
                                .getText());
                    } else {
                        docs = (ArrayList<Document>) root.findByName(
                                textQuery.getText(), department, departments);
                    }
                    break;
            }
            resultsSeachDocumentsId = new Document[docs.size()];
            for (int i = 0; i < docs.size(); i++) {
                resultsSeachDocumentsId[i] = docs.get(i);
            }

            MyTableModel model = (MyTableModel) tableResultsSearch.getModel();
            model.setResult(docs);
            model.fireTableDataChanged();
            actions.get(ACTION_CLEARRESULT).setEnabled(true);
        }

    }

    // слушатель кнопки снять выделение
    protected class ButtonClearResultsListener extends AbstractAction {

        public ButtonClearResultsListener() {
            putValue(Action.NAME, "Снять все выделения");
            putValue(Action.SHORT_DESCRIPTION,
                    "Cнять все выделение с найденых документов");
        }

        public void actionPerformed(ActionEvent arg0) {

            clearSearchResults();

        }

    }

    // слушатель кнопки создания окна
    protected class ButtonControlDepartmentListener extends AbstractAction {

        public ButtonControlDepartmentListener() {
            putValue(Action.NAME, "Управление подразделениями");
            putValue(Action.SHORT_DESCRIPTION,
                    "Создать новое или удалить существующее подразделение");
        }

        public void actionPerformed(ActionEvent arg0) {
            if (controlDepartmentFrame == null) {
                controlDepartmentFrame = new DocusControlDepartmentForm(mongo,
                        frame);
            }
        }

    }

    // слушатель кнопки создания окна
    protected class ButtonAppointDepartmentListener extends AbstractAction {

        public ButtonAppointDepartmentListener() {
            putValue(Action.NAME, "Назначить подразделение");
            putValue(Action.SHORT_DESCRIPTION,
                    "Определить принадлежность к какому-либо подразделению текущую папку");
        }

        public void actionPerformed(ActionEvent arg0) {
            if (appointDepartmentFrame == null) {
                appointDepartmentFrame = new DocusAppointDepartmentForm(mongo,
                        frame, bufferDoc);
            }
        }

    }

    // слушатель кнопки создания окна
    protected class ButtonControlUsersListener extends AbstractAction {

        public ButtonControlUsersListener() {
            putValue(Action.NAME, "Управление пользователями");
            putValue(Action.SHORT_DESCRIPTION,
                    "Наделить определенного пользователя определенными правами");
        }

        public void actionPerformed(ActionEvent arg0) {
            if (controlUserFrame == null) {
                controlUserFrame = new DocusControlUsersForm(mongo, frame);
            }
        }

    }

    // слушатель кнопки создания окна
    protected class ButtonControlWorkersListener extends AbstractAction {

        public ButtonControlWorkersListener() {
            putValue(Action.NAME, "Управление подчинеными");
            putValue(Action.SHORT_DESCRIPTION,
                    "Принять нового или уволить старого сотрудника своего подразделения");
        }

        public void actionPerformed(ActionEvent arg0) {
            if (chiefFrame == null) {
                chiefFrame = new DocusChiefForm(mongo, frame, department);
            }
        }

    }

    // слушатель кнопки создания окна
    protected class ButtonOpenCommentsListener extends AbstractAction {

        public ButtonOpenCommentsListener() {
            putValue(Action.NAME, "Коментарии");
            putValue(Action.SHORT_DESCRIPTION,
                    "Открыть окно комментариев к данному документу");
        }

        public void actionPerformed(ActionEvent arg0) {
            if (commentsFrame == null) {
                commentsFrame = new DocusComments(mongo, frame, userName,
                        department, currentSelectedDocument.getId());
            }
        }

    }

    // слушатель кнопки создания окна
    protected class ButtonOpenChangesListener extends AbstractAction {

        public ButtonOpenChangesListener() {
            putValue(Action.NAME, "Изменения");
            putValue(Action.SHORT_DESCRIPTION,
                    "Открыть окно просмотра изменений данного документа");
        }

        public void actionPerformed(ActionEvent arg0) {
            if (changesFrame == null) {
                changesFrame = new DocusChanges(frame,
                        currentSelectedDocument.getChanges());
            }
        }

    }

    // слушатель кнопки создания окна
    protected class ButtonControlDocumentsListener extends AbstractAction {

        public ButtonControlDocumentsListener() {
            putValue(Action.NAME, "Управление черновиками");
            putValue(Action.SHORT_DESCRIPTION,
                    "Одобрить или удалить черновой вариант документа");
        }

        public void actionPerformed(ActionEvent arg0) {

            if (reviewerFrame == null) {
                reviewerFrame = new DocusReviewerForm(mongo, frame,
                        departments, root);
            }
        }

    }

    // слушатель кнопки создания окна
    protected class ButtonControlReviewersListener extends AbstractAction {

        public ButtonControlReviewersListener() {
            putValue(Action.NAME, "Управление модераторами");
            putValue(Action.SHORT_DESCRIPTION,
                    "Принять нового или уволить старого сотрудника");
        }

        public void actionPerformed(ActionEvent arg0) {
            if (controlReviewerFrame == null) {
                controlReviewerFrame = new DocusControlReviewers(mongo, frame);
            }
        }

    }

    // слушатель кнопки о программе
    protected class Info extends AbstractAction implements ActionListener {
        public void actionPerformed(ActionEvent arg0) {

            JOptionPane
                    .showOptionDialog(
                            mainwindow,
                            "                           Хранилище документов v0.8 \n                    Поддерживаемая СУБД - \"MongoDB\" \n  `Если что-то не сломано - не исправляйте это` Рональд Рейган",
                            "Информация о программе", JOptionPane.OK_OPTION,
                            JOptionPane.INFORMATION_MESSAGE, null,
                            new Object[] { "Ок" }, "Ок");

        }
    }

    //статус бар
    public class StatusBar extends JLabel {
        private static final long serialVersionUID = 6678719349683873898L;

        public StatusBar() {
            super();
            super.setPreferredSize(new Dimension(100, 18));
            setMessage("Готов к работе");
        }

        public void setMessage(String message) {
            setText(" " + message);
        }
    }

    public class UserBar extends JLabel {
        private static final long serialVersionUID = 1L;

        public UserBar() {
            super();
            super.setPreferredSize(new Dimension(100, 18));
            super.setForeground(Color.blue);

        }

        public void getUserInfo() {
            String status = (chief) ? "начальник" : "работник";
            setText("Имя пользователя: " + userName + " | Подразделение: "
                    + department + " | Статус: " + status);
        }
    }

    protected class LoadFromDB implements ActionListener {

        public void actionPerformed(ActionEvent arg0) {
        }
    }

    // слушатель загрузки документов из XML
    protected class ButtonLoadDataListener extends AbstractAction {

        private static final long serialVersionUID = 1L;

        public ButtonLoadDataListener() {
            putValue(Action.NAME, "Загрузить БД документов");
            putValue(Action.SHORT_DESCRIPTION,
                    "Загрузить новую БД документов из XML файла");
        }

        public void actionPerformed(ActionEvent arg0) {
            XMLCreator xmlcreator = new XMLCreator();
            GridFS gfsDoc = new GridFS(db, "DocFiles");
            Boolean loaded = xmlcreator.loadDBfromXML(DocusFrame.this, table,
                    gfsDoc);
            DocusFrame.this.reload();
            if (loaded) {
                statusBar.setText("Новая БД документов успешно загружена");
            }
        }

    }

    // слушатель сохранения документов в XML
    protected class ButtonSaveDataListener extends AbstractAction {

        private static final long serialVersionUID = 1L;

        public ButtonSaveDataListener() {
            putValue(Action.NAME, "Сохранить БД документов");
            putValue(Action.SHORT_DESCRIPTION,
                    "Сохранить текущую БД документов в XML файл");
        }

        public void actionPerformed(ActionEvent arg0) {
            XMLCreator xmlcreator = new XMLCreator();
            GridFS gfsDoc = new GridFS(db, "DocFiles");
            if (xmlcreator.createXMLDocument(root.getAllDocuments(),
                    DocusFrame.this, gfsDoc)) {
                statusBar
                        .setText("БД документов успешно сохранены в локальный XML файл");
            }
        }
    }

    // слушатель сохранения пользователей и подразделений в XML
    protected class ButtonSaveUsersAndDepartmentsToXmlListener extends AbstractAction {

        private static final long serialVersionUID = 1L;

        public ButtonSaveUsersAndDepartmentsToXmlListener() {
            putValue(Action.NAME, "Сохранить БД пользователей и подразделений");
            putValue(Action.SHORT_DESCRIPTION,
                    "Сохранить текущую БД пользователей и подразделений в XML файл");
        }

        public void actionPerformed(ActionEvent arg0) {

        }
    }

    // слушатель загрузки пользователей и подразделений из XML
    protected class ButtonLoadUsersAndDepartmentsFromXmlListener extends AbstractAction {

        private static final long serialVersionUID = 1L;

        public ButtonLoadUsersAndDepartmentsFromXmlListener() {
            putValue(Action.NAME, "Загрузить БД пользователей и подразделений");
            putValue(Action.SHORT_DESCRIPTION,
                    "Загрузить БД пользователей и подразделений из XML файла");
        }

        public void actionPerformed(ActionEvent arg0) {

        }
    }


    protected class TableMouseListener implements MouseListener {

        @Override
        public void mouseClicked(MouseEvent arg0) {
            if (!resultSearch) {
                return;
            }
            bufferTrek = jTree.getSelectionPath();
            JTable select = (JTable) arg0.getComponent();
            Document doc = resultsSeachDocumentsId[select.getSelectedRow()];
            Document ancentor;
            TreePath trek = jTree.getPathForRow(0);
            List<Document> ancestors = doc.getAncestors();
            for (int i = 0; i < ancestors.size(); i++) {
                ancentor = ancestors.get(i);
                trek = trek.pathByAddingChild(ancentor);
            }
            trek = trek.pathByAddingChild(doc);
            jTree.setSelectionPath(trek);
            DocusMediator.appendResult(((Document) jTree
                    .getLastSelectedPathComponent()));
            jTree.setSelectionPath(bufferTrek);
        }

        @Override
        public void mouseEntered(MouseEvent arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void mouseExited(MouseEvent arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void mousePressed(MouseEvent arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void mouseReleased(MouseEvent arg0) {
            // TODO Auto-generated method stub

        }
    }

    // слушатель кнопки создания окна
    protected class ButtonEnterListener extends AbstractAction {

        public ButtonEnterListener() {
            putValue(Action.NAME, "Открыть окно входа");
            putValue(Action.SHORT_DESCRIPTION,
                    "Здесь вы можите войти в систему под своим логином");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            if (loginFrame == null) {
                loginFrame = new DocusAuthorizationForm(mongo, frame);
            }
        }

    }

    // выход пользователя
    protected class ButtonOutListener extends AbstractAction {

        public ButtonOutListener() {
            putValue(Action.NAME, "Выйти");
            putValue(Action.SHORT_DESCRIPTION, "Выйти и получить права гостя");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!userName.equals("unregistered")) {
                closeAllChildsWindows();
                statusBar.setText(userName + " вышел из системы.");
                enterHowUnregistred();
                userBar.getUserInfo();
                disableAllButtons();
                initFilterDocButtonsForUser();
                resetFlags();
                if (searchFrame != null) {
                    searchFrame.dispose();
                    searchFrame = null;
                }
                getAdminMenu();
                closeProhibitedFolders();
            }

        }
    }

    // слушатель изменения окна
    protected class WindowChangeSizeListener implements ComponentListener {

        @Override
        public void componentHidden(ComponentEvent arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void componentMoved(ComponentEvent arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void componentResized(ComponentEvent arg0) {
            tableResultsSearch.setSize(dimension);
            Integer newHeightWindow = arg0.getComponent().getHeight();
            if (newHeightWindow > 439) {
                tableResultsSearch.setFillsViewportHeight(true);
                dimension.height = ((int) dimension.getHeight()
                        + newHeightWindow - heightWindow);
                tableResultsSearch
                        .setPreferredScrollableViewportSize(dimension);
                tableResultsSearch.setSize(dimension);
                heightWindow = newHeightWindow;
            }
        }

        @Override
        public void componentShown(ComponentEvent arg0) {
            // TODO Auto-generated method stub

        }

    }

    // слушатель кнопки создания окна
    protected class ButtonRegistrationListener extends AbstractAction {

        public ButtonRegistrationListener() {
            putValue(Action.NAME, "Зарегистрироваться");
            putValue(Action.SHORT_DESCRIPTION,
                    "Зарегистрировать нового пользователя");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (registrationFrame == null) {
                registrationFrame = new DocusRegistrationForm(mongo, frame);
            }
        }

    }

    // слушатель расширения дерева
    protected class TreeExpandListener implements TreeWillExpandListener {

        @Override
        public void treeWillExpand(TreeExpansionEvent event)
                throws ExpandVetoException {
            TreePath path = event.getPath();
            Document doc = (Document) path.getLastPathComponent();
            Boolean allowExpand = false;
            String docDepartment = doc.getDepartment();
            if (docDepartment.equals("general")
                    || docDepartment.equals(department)
                    || (department.equals("general") && chief)) {
                allowExpand = true;
            } else if (department.equals("reviewers")) {
                allowExpand = checkDepartmentForReviewers(docDepartment);
            }
            if (!allowExpand) {
                throw new ExpandVetoException(event);
            }
        }

        @Override
        public void treeWillCollapse(TreeExpansionEvent event)
                throws ExpandVetoException {
            // throw new ExpandVetoException(event);
        }
    };

    // рендер
    class MyRenderer extends DefaultTreeCellRenderer {

        public MyRenderer() {
        }

        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean sel, boolean expanded, boolean leaf, int row,
                                                      boolean hasFocus) {

            super.getTreeCellRendererComponent(tree, value, sel, expanded,
                    leaf, row, hasFocus);

            Document currentDocument = (Document) value;

            if (cutDocFlag && currentDocument.equals(cutDocument)) {
                this.setBackgroundNonSelectionColor(Color.cyan);
            } else if (DocusMediator.contatinsDoc(currentDocument)) {
                this.setBackgroundNonSelectionColor(Color.yellow);
            } else {
                this.setBackgroundNonSelectionColor(null);
            }
            return this;
        }

    }

    // Заполнение таблицы
    class MyTableModel extends AbstractTableModel {

        private String[] columnNames = { "Название документа", "Автор",
                "Последний вызов", "Файл", };

        public MyTableModel() {
            data = new Object[][] { { "", "", "", new Boolean(false) } };

        }

        public void setResult(ArrayList<Document> docs) {

            if (docs.size() == 0) {
                resultSearch = false;
                data = new Object[][] { { "", "", "", new Boolean(false) } };
                statusBar.setText("По вашему запросу ничего не найдено");
                return;

            }
            resultSearch = true;
            data = new Object[docs.size()][4];
            int i = 0;
            Document currentDoc;
            while (i < docs.size()) {
                currentDoc = docs.get(i);

                data[i][0] = currentDoc.getName();
                data[i][1] = currentDoc.getAuthor();
                data[i][2] = currentDoc.getModified();
                data[i][3] = currentDoc.getFile();
                i++;
            }

            statusBar.setText("По вашему запросу найдено " + docs.size()
                    + " записей.");

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

    // кодированеи пароля
    public static String encodePassword(String password, String salt) {
        try {
            String saltedAndHashed = password + "," + salt;
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(saltedAndHashed.getBytes());
            byte hashedBytes[] = (new String(digest.digest(), "UTF-8"))
                    .getBytes();
            return DatatypeConverter.printBase64Binary(hashedBytes) + ","
                    + salt;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 is not available", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 unavailable?  Not a chance", e);
        }
    }

}

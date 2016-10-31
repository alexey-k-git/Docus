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
import java.util.Stack;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.AbstractTableModel;

import org.bson.types.ObjectId;

import ru.restfulrobot.docus.DocTree.Document;
import ru.restfulrobot.docus.DocusFrame.MyTableModel;
import ru.restfulrobot.docus.DocusFrame.TableMouseListener;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

public class DocusChanges extends JFrame {

    private DocusFrame mainFrame;
    private JTable changesTable;
    private JPanel tablePanel;
    private JScrollPane spTablePanel;
    private JPanel mainPanel;

    private Stack<DocusChangeDocument> changes;

    public DocusChanges(DocusFrame mainFrame, Stack<DocusChangeDocument> changes) {
        super();
        init(mainFrame, changes);
    }

    public void init(DocusFrame docusFrame, Stack<DocusChangeDocument> changes) {
        this.mainFrame = docusFrame;
        this.mainFrame.setEnabled(false);
        this.changes = changes;
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mainFrame.setEnabled(true);
                mainFrame.removeChangesForm();
            }
        });
        mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBackground(Color.white);
        tablePanel = new JPanel();
        Dimension dimension = new Dimension(390, 170);
        changesTable = new JTable(new ChangesTableModel());
        changesTable.setPreferredScrollableViewportSize(dimension);
        changesTable.setFillsViewportHeight(true);
        tablePanel.add(changesTable);
        spTablePanel = new JScrollPane(changesTable);
        mainPanel.add(spTablePanel, BorderLayout.CENTER);
        update();
        setTitle("Просмотр изменений документа");
        setContentPane(mainPanel);
        setSize(600, 600);
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);

    }


    private void update() {

        ChangesTableModel model = (ChangesTableModel) changesTable.getModel();
        model.setResult(changes);
        model.fireTableDataChanged();

    }

    class ChangesTableModel extends AbstractTableModel {

        private String[] columnNames = { "Имя пользователя", "Подразделение",
                "Изменение", "Заменён файл", };

        public ChangesTableModel() {
            data = new Object[][] { { "", "", "", new Boolean(false) } };

        }

        public void setResult(Stack<DocusChangeDocument> changes) {

            if (changes.size() == 0) {
                data = new Object[][] { { "", "", "", new Boolean(false) } };
                return;

            }
            data = new Object[changes.size()][4];
            int i = 0;
            DocusChangeDocument currentDoc;
            while (i < changes.size()) {
                currentDoc = changes.elementAt(i);
                data[i][0] = currentDoc.getName();
                data[i][1] = currentDoc.getDepartment();
                Date date = currentDoc.getDate();
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm aa");
                String mydateStr = df.format(date);
                data[i][2] = mydateStr;
                data[i][3] = currentDoc.getChangeFile();
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

}
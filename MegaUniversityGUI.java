import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/*
 * MegaUniversityGUI.java
 * Single-file Swing GUI "mega" program with:
 * 1) Simulated Object-Oriented Database (OODB) using per-object .ser files
 * 2) XML export/import for data sharing (served/consumed by SiteA/SiteB)
 *
 * How to run:
 *   javac MegaUniversityGUI.java
 *   java MegaUniversityGUI
 *
 * This file **does not** need any external libraries.
 */

interface Displayable { void display(); }

abstract class Person implements Serializable {
    private static final long serialVersionUID = 1L;
    String name;
    int age;
    Person(String name, int age) { this.name = name; this.age = age; }
    abstract void showRole();
}

class Student extends Person implements Displayable {
    private static final long serialVersionUID = 1L;
    int rollNo;
    String course;

    Student(int rollNo, String name, int age, String course) {
        super(name, age);
        this.rollNo = rollNo;
        this.course = course;
    }
    public void display() {
        System.out.println("Student RollNo: " + rollNo + ", Name: " + name + ", Age: " + age + ", Course: " + course);
    }
    void showRole() { System.out.println(name + " is a Student."); }
}

class Faculty extends Person implements Displayable {
    private static final long serialVersionUID = 1L;
    String facultyId;
    String subject;

    Faculty(String facultyId, String name, int age, String subject) {
        super(name, age);
        this.facultyId = facultyId;
        this.subject = subject;
    }
    public void display() {
        System.out.println("Faculty ID: " + facultyId + ", Name: " + name + ", Age: " + age + ", Subject: " + subject);
    }
    void showRole() { System.out.println(name + " is a Faculty."); }
}

/* -------------------------------
   Simulated OODB (pure Java)
   - One file per object
   - Directory layout:
       odb/
         students/rollNo-<id>.ser
         faculties/facultyId-<id>.ser
   - List = directory listing
   - Insert/Update = overwrite file
   - Delete = delete file
-------------------------------- */
class FileOodb {
    private final Path root;
    private final Path studentsDir;
    private final Path facultiesDir;

    FileOodb(String rootDir) {
        this.root = Paths.get(rootDir);
        this.studentsDir = root.resolve("students");
        this.facultiesDir = root.resolve("faculties");
        try {
            Files.createDirectories(studentsDir);
            Files.createDirectories(facultiesDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to init OODB dirs: " + e.getMessage(), e);
        }
    }

    /* ---------- Students ---------- */
    void upsertStudent(Student s) {
        Path p = studentsDir.resolve("rollNo-" + s.rollNo + ".ser");
        writeObject(p, s);
    }

    void deleteStudent(int rollNo) {
        Path p = studentsDir.resolve("rollNo-" + rollNo + ".ser");
        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
    }

    List<Student> loadAllStudents() {
        try {
            if (!Files.exists(studentsDir)) return new ArrayList<>();
            List<Path> files = Files.list(studentsDir)
                    .filter(x -> x.getFileName().toString().startsWith("rollNo-") && x.toString().endsWith(".ser"))
                    .collect(Collectors.toList());
            List<Student> res = new ArrayList<>();
            for (Path f : files) {
                Student s = (Student) readObject(f);
                if (s != null) res.add(s);
            }
            return res;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    /* ---------- Faculties ---------- */
    void upsertFaculty(Faculty f) {
        Path p = facultiesDir.resolve("facultyId-" + safe(f.facultyId) + ".ser");
        writeObject(p, f);
    }

    void deleteFaculty(String facultyId) {
        Path p = facultiesDir.resolve("facultyId-" + safe(facultyId) + ".ser");
        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
    }

    List<Faculty> loadAllFaculties() {
        try {
            if (!Files.exists(facultiesDir)) return new ArrayList<>();
            List<Path> files = Files.list(facultiesDir)
                    .filter(x -> x.getFileName().toString().startsWith("facultyId-") && x.toString().endsWith(".ser"))
                    .collect(Collectors.toList());
            List<Faculty> res = new ArrayList<>();
            for (Path f : files) {
                Faculty fac = (Faculty) readObject(f);
                if (fac != null) res.add(fac);
            }
            return res;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    /* ---------- Helpers ---------- */
    private void writeObject(Path file, Object obj) {
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(file))) {
            oos.writeObject(obj);
        } catch (IOException e) {
            System.err.println("[OODB] Write failed: " + e.getMessage());
        }
    }

    private Object readObject(Path file) {
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file))) {
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[OODB] Read failed: " + e.getMessage());
            return null;
        }
    }

    private static String safe(String s) {
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

/* -------------------------------
   XML Export/Import using DOM (no JAXB dependency)
   Files: students.xml, faculty.xml
-------------------------------- */
class XmlIO {

    /* ---------- Export ---------- */
    static void exportStudents(List<Student> students, File out) {
        try {
            Document doc = newDoc();
            Element root = doc.createElement("students");
            doc.appendChild(root);

            for (Student s : students) {
                Element e = doc.createElement("student");

                append(doc, e, "rollNo", String.valueOf(s.rollNo));
                append(doc, e, "name", s.name);
                append(doc, e, "age", String.valueOf(s.age));
                append(doc, e, "course", s.course);

                root.appendChild(e);
            }
            write(doc, out);
        } catch (Exception e) {
            throw new RuntimeException("Export students failed: " + e.getMessage(), e);
        }
    }

    static void exportFaculties(List<Faculty> faculties, File out) {
        try {
            Document doc = newDoc();
            Element root = doc.createElement("faculties");
            doc.appendChild(root);

            for (Faculty f : faculties) {
                Element e = doc.createElement("faculty");

                append(doc, e, "facultyId", f.facultyId);
                append(doc, e, "name", f.name);
                append(doc, e, "age", String.valueOf(f.age));
                append(doc, e, "subject", f.subject);

                root.appendChild(e);
            }
            write(doc, out);
        } catch (Exception e) {
            throw new RuntimeException("Export faculties failed: " + e.getMessage(), e);
        }
    }

    /* ---------- Import ---------- */
    static List<Student> importStudents(File in) {
        List<Student> list = new ArrayList<>();
        if (!in.exists()) return list;
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
            NodeList nodes = doc.getElementsByTagName("student");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = (Element) nodes.item(i);
                int rollNo = Integer.parseInt(text(e, "rollNo"));
                String name = text(e, "name");
                int age = Integer.parseInt(text(e, "age"));
                String course = text(e, "course");
                list.add(new Student(rollNo, name, age, course));
            }
        } catch (Exception e) {
            throw new RuntimeException("Import students failed: " + e.getMessage(), e);
        }
        return list;
    }

    static List<Faculty> importFaculties(File in) {
        List<Faculty> list = new ArrayList<>();
        if (!in.exists()) return list;
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
            NodeList nodes = doc.getElementsByTagName("faculty");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = (Element) nodes.item(i);
                String id = text(e, "facultyId");
                String name = text(e, "name");
                int age = Integer.parseInt(text(e, "age"));
                String subject = text(e, "subject");
                list.add(new Faculty(id, name, age, subject));
            }
        } catch (Exception e) {
            throw new RuntimeException("Import faculties failed: " + e.getMessage(), e);
        }
        return list;
    }

    /* ---------- DOM helpers ---------- */
    private static Document newDoc() throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    }
    private static void append(Document doc, Element parent, String tag, String text) {
        Element t = doc.createElement(tag);
        t.appendChild(doc.createTextNode(text == null ? "" : text));
        parent.appendChild(t);
    }
    private static void write(Document doc, File out) throws Exception {
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        try (OutputStream os = new FileOutputStream(out)) {
            tf.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(os, StandardCharsets.UTF_8)));
        }
    }
    private static String text(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return "";
        return nl.item(0).getTextContent();
    }
}

/* -------------------------------
   GUI + OODB + XML
-------------------------------- */
public class MegaUniversityGUI extends JFrame {
    // In-memory lists (UI model)
    private final List<Student> students = Collections.synchronizedList(new ArrayList<>());
    private final List<Faculty> faculties = Collections.synchronizedList(new ArrayList<>());
    private final DefaultTableModel studentTableModel = new DefaultTableModel(new String[]{"RollNo", "Name", "Age", "Course"}, 0);
    private final DefaultTableModel facultyTableModel = new DefaultTableModel(new String[]{"FacultyID", "Name", "Age", "Subject"}, 0);

    private final JTable studentTable = new JTable(studentTableModel);
    private final JTable facultyTable = new JTable(facultyTableModel);

    // Simulated OODB (pure Java)
    private final FileOodb oodb = new FileOodb("odb");

    public MegaUniversityGUI() {
        super("Mega University System (GUI) – OODB + XML");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 640);
        setLocationRelativeTo(null);
        initMenu();
        initUI();
        loadDataOnStart();
    }

    private void initMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu file = new JMenu("File");

        JMenuItem exportXml = new JMenuItem("Export to XML");
        exportXml.addActionListener(e -> {
            synchronized (students) { XmlIO.exportStudents(students, new File("students.xml")); }
            synchronized (faculties) { XmlIO.exportFaculties(faculties, new File("faculty.xml")); }
            JOptionPane.showMessageDialog(this, "Exported: students.xml & faculty.xml");
        });

        JMenuItem importXml = new JMenuItem("Import from XML");
        importXml.addActionListener(e -> {
            File sFile = new File("students.xml");
            File fFile = new File("faculty.xml");
            if (!sFile.exists() && !fFile.exists()) {
                JOptionPane.showMessageDialog(this, "No XML files found beside app.");
                return;
            }

            if (sFile.exists()) {
                List<Student> sList = XmlIO.importStudents(sFile);
                synchronized (students) { students.clear(); students.addAll(sList); }
                // Sync to OODB
                for (Student s : sList) oodb.upsertStudent(s);
            }
            if (fFile.exists()) {
                List<Faculty> fList = XmlIO.importFaculties(fFile);
                synchronized (faculties) { faculties.clear(); faculties.addAll(fList); }
                for (Faculty f : fList) oodb.upsertFaculty(f);
            }
            refreshTables();
            JOptionPane.showMessageDialog(this, "Imported from XML (and persisted to OODB).");
        });

        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> System.exit(0));

        file.add(exportXml);
        file.add(importXml);
        file.addSeparator();
        file.add(exit);
        menuBar.add(file);
        setJMenuBar(menuBar);
    }

    private void initUI() {
        JTabbedPane tabs = new JTabbedPane();

        // STUDENT TAB
        JPanel studentPanel = new JPanel(new BorderLayout());
        studentPanel.add(createStudentForm(), BorderLayout.NORTH);
        studentTable.setFillsViewportHeight(true);
        studentPanel.add(new JScrollPane(studentTable), BorderLayout.CENTER);
        JPanel studentButtons = new JPanel();
        JButton removeStudentBtn = new JButton("Remove Selected Student");
        removeStudentBtn.addActionListener(e -> removeSelectedStudent());
        studentButtons.add(removeStudentBtn);
        studentPanel.add(studentButtons, BorderLayout.SOUTH);

        // FACULTY TAB
        JPanel facultyPanel = new JPanel(new BorderLayout());
        facultyPanel.add(createFacultyForm(), BorderLayout.NORTH);
        facultyTable.setFillsViewportHeight(true);
        facultyPanel.add(new JScrollPane(facultyTable), BorderLayout.CENTER);
        JPanel facultyButtons = new JPanel();
        JButton removeFacultyBtn = new JButton("Remove Selected Faculty");
        removeFacultyBtn.addActionListener(e -> removeSelectedFaculty());
        facultyButtons.add(removeFacultyBtn);
        facultyPanel.add(facultyButtons, BorderLayout.SOUTH);

        tabs.addTab("Students", studentPanel);
        tabs.addTab("Faculty", facultyPanel);

        add(tabs, BorderLayout.CENTER);

        JLabel status = new JLabel("OODB: odb/  | XML: students.xml, faculty.xml  | Websites: SiteA(8081), SiteB(8082)");
        add(status, BorderLayout.SOUTH);
    }

    private JPanel createStudentForm() {
        JPanel form = new JPanel();
        form.setBorder(BorderFactory.createTitledBorder("Add Student"));
        form.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4);

        c.gridx = 0; c.gridy = 0; form.add(new JLabel("Roll No"), c);
        c.gridx = 1; JTextField rollField = new JTextField(8); form.add(rollField, c);
        c.gridx = 0; c.gridy = 1; form.add(new JLabel("Name"), c);
        c.gridx = 1; JTextField nameField = new JTextField(15); form.add(nameField, c);
        c.gridx = 0; c.gridy = 2; form.add(new JLabel("Age"), c);
        c.gridx = 1; JTextField ageField = new JTextField(5); form.add(ageField, c);
        c.gridx = 0; c.gridy = 3; form.add(new JLabel("Course"), c);
        c.gridx = 1; JTextField courseField = new JTextField(12); form.add(courseField, c);

        c.gridx = 0; c.gridy = 4; c.gridwidth = 2;
        JButton addBtn = new JButton("Add Student");
        form.add(addBtn, c);

        addBtn.addActionListener(e -> {
            try {
                int roll = Integer.parseInt(rollField.getText().trim());
                String name = nameField.getText().trim();
                int age = Integer.parseInt(ageField.getText().trim());
                String course = courseField.getText().trim();
                if (name.isEmpty() || course.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Name and Course are required.");
                    return;
                }
                Student s = new Student(roll, name, age, course);
                synchronized (students) { students.add(s); }
                studentTableModel.addRow(new Object[]{roll, name, age, course});

                // Persist to OODB immediately
                oodb.upsertStudent(s);

                rollField.setText(""); nameField.setText(""); ageField.setText(""); courseField.setText("");
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Invalid numeric input for Roll No or Age.");
            }
        });

        return form;
    }

    private JPanel createFacultyForm() {
        JPanel form = new JPanel();
        form.setBorder(BorderFactory.createTitledBorder("Add Faculty"));
        form.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4);

        c.gridx = 0; c.gridy = 0; form.add(new JLabel("Faculty ID"), c);
        c.gridx = 1; JTextField idField = new JTextField(8); form.add(idField, c);
        c.gridx = 0; c.gridy = 1; form.add(new JLabel("Name"), c);
        c.gridx = 1; JTextField nameField = new JTextField(15); form.add(nameField, c);
        c.gridx = 0; c.gridy = 2; form.add(new JLabel("Age"), c);
        c.gridx = 1; JTextField ageField = new JTextField(5); form.add(ageField, c);
        c.gridx = 0; c.gridy = 3; form.add(new JLabel("Subject"), c);
        c.gridx = 1; JTextField subjField = new JTextField(12); form.add(subjField, c);

        c.gridx = 0; c.gridy = 4; c.gridwidth = 2;
        JButton addBtn = new JButton("Add Faculty");
        form.add(addBtn, c);

        addBtn.addActionListener(e -> {
            try {
                String id = idField.getText().trim();
                String name = nameField.getText().trim();
                int age = Integer.parseInt(ageField.getText().trim());
                String subj = subjField.getText().trim();
                if (id.isEmpty() || name.isEmpty() || subj.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "All fields are required.");
                    return;
                }
                Faculty f = new Faculty(id, name, age, subj);
                synchronized (faculties) { faculties.add(f); }
                facultyTableModel.addRow(new Object[]{id, name, age, subj});

                // Persist to OODB
                oodb.upsertFaculty(f);

                idField.setText(""); nameField.setText(""); ageField.setText(""); subjField.setText("");
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Invalid numeric input for Age.");
            }
        });

        return form;
    }

    private void removeSelectedStudent() {
        int sel = studentTable.getSelectedRow();
        if (sel >= 0) {
            int roll = (int) studentTableModel.getValueAt(sel, 0);
            synchronized (students) { students.removeIf(s -> s.rollNo == roll); }
            studentTableModel.removeRow(sel);
            // Remove from OODB
            oodb.deleteStudent(roll);
        } else {
            JOptionPane.showMessageDialog(this, "Select a student row first.");
        }
    }

    private void removeSelectedFaculty() {
        int sel = facultyTable.getSelectedRow();
        if (sel >= 0) {
            String id = (String) facultyTableModel.getValueAt(sel, 0);
            synchronized (faculties) { faculties.removeIf(f -> f.facultyId.equals(id)); }
            facultyTableModel.removeRow(sel);
            // Remove from OODB
            oodb.deleteFaculty(id);
        } else {
            JOptionPane.showMessageDialog(this, "Select a faculty row first.");
        }
    }

    private void loadDataOnStart() {
        // Load from OODB
        List<Student> sList = oodb.loadAllStudents();
        List<Faculty> fList = oodb.loadAllFaculties();
        synchronized (students) { students.clear(); students.addAll(sList); }
        synchronized (faculties) { faculties.clear(); faculties.addAll(fList); }
        refreshTables();
    }

    private void refreshTables() {
        SwingUtilities.invokeLater(() -> {
            studentTableModel.setRowCount(0);
            facultyTableModel.setRowCount(0);
            synchronized (students) {
                for (Student s : students) {
                    studentTableModel.addRow(new Object[]{s.rollNo, s.name, s.age, s.course});
                }
            }
            synchronized (faculties) {
                for (Faculty f : faculties) {
                    facultyTableModel.addRow(new Object[]{f.facultyId, f.name, f.age, f.subject});
                }
            }
        });
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> {
            MegaUniversityGUI app = new MegaUniversityGUI();
            app.setVisible(true);
        });
    }
}

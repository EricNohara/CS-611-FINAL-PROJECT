import javax.swing.*;
import java.awt.*;

class DashboardFrame extends JFrame {
    public DashboardFrame(String role) {
        setTitle(role + " Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        JMenuBar menuBar = new JMenuBar();
        JMenu courseMenu = new JMenu("Courses");
        JMenuItem newClassItem = new JMenuItem("Create New Class");
        JMenuItem copyClassItem = new JMenuItem("Copy from Previous Class");
        courseMenu.add(newClassItem);
        courseMenu.add(copyClassItem);
        menuBar.add(courseMenu);

        JMenu gradeMenu = new JMenu("Grades");
        JMenuItem viewGradesItem = new JMenuItem("View Grades");
        JMenuItem statisticsItem = new JMenuItem("Grade Statistics");
        gradeMenu.add(viewGradesItem);
        gradeMenu.add(statisticsItem);
        menuBar.add(gradeMenu);

        JMenu fileMenu = new JMenu("Submissions");
        JMenuItem uploadItem = new JMenuItem("Upload Submission");
        fileMenu.add(uploadItem);
        menuBar.add(fileMenu);

        JMenu userMenu = new JMenu("Users");
        JMenuItem manageUsers = new JMenuItem("Manage Roles");
        userMenu.add(manageUsers);
        menuBar.add(userMenu);

        JMenu toolsMenu = new JMenu("Tools");
        JMenuItem similarityCheck = new JMenuItem("Run Similarity Check");
        toolsMenu.add(similarityCheck);
        menuBar.add(toolsMenu);

        setJMenuBar(menuBar);

        JTextArea infoArea = new JTextArea("Welcome, " + role + "!\nSelect an option from the menu to get started.");
        infoArea.setEditable(false);
        add(new JScrollPane(infoArea), BorderLayout.CENTER);

        setVisible(true);
    }
}

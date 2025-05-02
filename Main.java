// Main entrypoint into the application - makes and runs the grading system app

import db.DBSetup;

public class Main {
    public static void main(String[] args) {
        // DBSetup.clearAllTables();
        GradingSystemApp app = new GradingSystemApp();
        app.run();
    }
}

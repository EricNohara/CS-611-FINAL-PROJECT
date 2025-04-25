package ui.utils;

public interface Refreshable {
    /** Re-query the DB and repopulate tables / cards / combo-boxes. */
    void refreshData();
}
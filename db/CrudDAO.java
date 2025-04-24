package db;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface CrudDAO<T> {
    void create(T t);
    T read(int id); // read one row with given id
    List<T> readAllCondition(String columnName, Object value); // get all rows following given constraint
    List<T> readAll(); // get every single row in table
    void update(T t);
    void delete(int id);
    T buildFromResultSet(ResultSet rs) throws SQLException;
}

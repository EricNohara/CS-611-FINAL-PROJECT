
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface CrudDAO<T> {
    T read(int id);
    List<T> readAll();
    void create(T t);
    void update(T t);
    void delete(int id);
    T buildFromResultSet(ResultSet rs) throws SQLException;
}

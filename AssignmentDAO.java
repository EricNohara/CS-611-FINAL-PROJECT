import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AssignmentDAO implements CrudDAO<Assignment> {
    // ABSTRACT CRUD OPERATIONS IMPLEMENTATIONS
    
    @Override
    public T read(int id);
    
    @Override
    public List<T> readAll();
    
    @Override
    public void create(T t);
    
    @Override
    public void update(T t);
    
    @Override
    public void delete(int id);
    
    @Override
    public T buildFromResultSet(ResultSet rs) throws SQLException;
}

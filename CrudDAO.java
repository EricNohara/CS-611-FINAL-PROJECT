import java.util.List;

public interface CrudDAO<T> {
    T read(int id);
    List<T> readAll();
    void create(T t);
    void update(T t);
    void delete(int id);
}

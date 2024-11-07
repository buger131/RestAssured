package utils;

import org.springframework.jdbc.datasource.DriverManagerDataSource;
import javax.sql.DataSource;

import static utils.Props.getProperty;

/**------------------------------------------------
 * В данном классе описан механизм
 * подключения к базе данных, а так же
 * загрузки драйвера и передача основных параметров
 * возвращаемый параметр типа dataSource
 * используется для передачи в JdbcTemplate
 * -----------------------------------------------*/

public class Database {
    private static final String url = getProperty("database_url");
    private static final String username = getProperty("database_user");
    private static final String password = getProperty("database_password");

    public static DataSource pgDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }
}

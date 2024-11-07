package api;

import api.extensions.BeforeEachExtension;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.odftoolkit.odfdom.doc.OdfDocument;
import org.odftoolkit.odfdom.pkg.OdfElement;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import utils.Database;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static api.extensions.BeforeEachExtension.*;
import static utils.Props.getProperty;

/** ---------------------------------------------------------
 * Основной класс для работы тестов.
 * В нем производится инициализация основных переменных,
 * описаны методы которые выполняются до запуска АТ (setUp())
 * и после (tearDown()), так же описаны методы инициализации
 * подключения к БД и работы с ней
 * ----------------------------------------------------------*/

@ExtendWith({BeforeEachExtension.class})
public class BaseTest {

    private static RequestSpecification requestSpecification;
    private static JdbcTemplate jdbcTemplate;

    private static final JSONParser parser = new JSONParser();

    private static final String protocol = getProperty("protocol");
    private static final String base_uri = getProperty("base_uri");

    @BeforeAll
    static void setJdbcTemplate() {
        jdbcTemplate = new JdbcTemplate(Database.pgDataSource());
    }

    @BeforeEach
    void setUp() {
        requestSpecification = RestAssured.given()
                .baseUri(protocol + "://" + base_uri)
                .auth()
                .basic(user.login(), user.password())
                .accept(ContentType.JSON);
    }

    //JSON в тело запроса передается файлом, указывается путь к файлу в формате /src/...,
    //извлекает ответ для последующей обработки
    public static Response extractResponse(String url, String path, String contentType) {

        File file;
        File none = new File("src/test/resources/none.json");

        if (!Objects.equals(path, "")) {
            file = new File(path);
        } else {
            file = none;
        }

        return RestAssured.given(requestSpecification)
                .filter(new AllureRestAssured())
                .body(file)
                .get(url)
                .then()
                .contentType(contentType)
                .extract()
                .response();
    }

    //Перегрузка метода для передачи параметров в виде params (HashMap)
    public static Response extractResponse(String url, Map<String, String> map) {

        return RestAssured.given(requestSpecification)
                .params(map)
                .filter(new AllureRestAssured())
                .get(url)
                .then()
                .extract()
                .response();
    }

    //JSON в тело запроса передается в формате JSONString (JSONObject.toJSONString())
    public static Response request(String url, String json) {
        return RestAssured.given(requestSpecification)
                .filter(new AllureRestAssured())
                .body(json)
                .get(url);
    }

    //Парсинг данных из JSON файла, для использования в тестах см. parseJsonData()
    private String parseJsonDataFile(String path) throws IOException, ParseException {
        JSONObject obj = (JSONObject) parser.parse(new FileReader(path));
        return obj.toJSONString();
    }

    //Метод для парсинга данных полученных в теле ответа, если необходимо вытащить данные по из определенного массива
    //по ключу см. getResponseData() - в тестах необходимо использовать именного его
    private static String getJsonResponseData(String url,
                                              String path,
                                              String contentType,
                                              String array_name,
                                              int index,
                                              String parameter_name) throws ParseException {
        LinkedHashMap<String, String> jsonPath = extractResponse(url, path, contentType)
                .jsonPath()
                .get("$");
        JSONArray arr = (JSONArray) parser.parse(jsonPath.get(array_name));
        JSONObject object = (JSONObject) parser.parse(arr.get(index).toString());
        return object.get(parameter_name).toString();
    }

    //Метод для парсинга и замены данных в массиве JSON, возвращает измененный ФАЙЛ, нет необходимости в конвертации
    //для использования в тестах см. updateJsonData(), есть возможность добавлять неограниченное количество аргументов,
    //в цикле происходит перебор аргументов по 2, подставляет сначала 1 и 2 элемент затем 3 и 4 и т.д.
    //массив всегда должен содержать в себе четное количество элементов.
    //При передаче в array_name значения null объекты будут добавлены в корень файла json.
    @SuppressWarnings("unchecked")
    private static String putJsonData(String json,
                                      String array_name,
                                      int index,
                                      String @NotNull ... param) throws ParseException, IOException {
        JSONObject file = (JSONObject) parser.parse(json);
        if (array_name == null) {
            for (int i = 0; i < param.length; i+=2) {
                file.put(param[i], param[i+1]);
            }
        } else {
            JSONArray arr = (JSONArray) file.get(array_name);
            JSONObject data = (JSONObject) arr.get(index);
            for (int i = 0; i < param.length; i += 2) {
                data.put(param[i], param[i + 1]);
            }
        }
        return file.toJSONString();
    }

    public String parseJsonData(String path) {
        try {
            return parseJsonDataFile(path);
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public String getResponseData(String url, String path, String contentType, String array_name, int index,
                                  String parameter_name) {
        try {
            return getJsonResponseData(url, path, contentType, array_name, index, parameter_name);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public String updateJsonData(String path, String array_name, int index, String ... param) {
        try {
            return putJsonData(path, array_name, index, param);
        } catch (ParseException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    //Метод для получения файла content.xml содержащегося в печатной форме
    //После выполнения метода, необходимо осуществить поиск по файлу средствами OdfDocument
    public OdfElement getPrintedRoot(@NotNull Response response) {
        InputStream inputStream = new ByteArrayInputStream(response.asByteArray());
        try {
            OdfDocument odfDocument = OdfDocument.loadDocument(inputStream);
            return odfDocument.getContentRoot();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**--------------------------------------
     * Методы для работы с БД
     * --------------------------------------*/

    public <T> T getDatabaseTableValue(String sqlStatement, Class<T> requiredType) {
        try {
            return jdbcTemplate.queryForObject(sqlStatement, requiredType);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @AfterAll
    static void tearDown() {
        requestSpecification = null;
        jdbcTemplate = null;
    }
}

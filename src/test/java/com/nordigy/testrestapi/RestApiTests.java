package com.nordigy.testrestapi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// It allows to refresh context(Database) before an each method. So your tests always will be executed on the same snapshot of DB.
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
class RestApiTests {

    @LocalServerPort
    private int port;

    @PostConstruct
    public void init() {
        RestAssured.port = port;
    }

    @Test
    public void shouldReturnCorrectUsersListSize() {
        given().log().all()
                .queryParam("size", 2)
                .when().get("/api/users")
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("page.totalPages", is(10));
    }

    @Test
    public void shouldCreateNewUser() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("firstName", "Ivan");
        objectNode.put("lastName", "Ivanov");
        objectNode.put("dayOfBirth", "2000-01-01");
        objectNode.put("email", "asdas@asdas.tr");

        ObjectNode user = given().log().all()
                .body(objectNode)
                .contentType(ContentType.JSON)
                .when().post("/api/users")
                .then().log().ifValidationFails()
                .statusCode(201)
                .extract().body().as(ObjectNode.class);

        assertThat(user.get("firstName")).isEqualTo(objectNode.get("firstName"));
        assertThat(user.get("lastName")).isEqualTo(objectNode.get("lastName"));
        assertThat(user.get("dayOfBirth")).isEqualTo(objectNode.get("dayOfBirth"));
        assertThat(user.get("email")).isEqualTo(objectNode.get("email"));
        assertThat(user.get("id").asLong()).isGreaterThan(20);
    }

    // TODO: The test methods above are examples of test cases.

    //  Please add new cases below, but don't hesitate to refactor the whole class.
    private static Stream<Arguments> providerForTotalPages() {
        return Stream.of(
                Arguments.of("1", 20),
                Arguments.of("2", 10),
                Arguments.of("3", 7),
                Arguments.of("20", 1),
                Arguments.of("100", 1),
                Arguments.of("-1", 1),
                Arguments.of("asd", 1),
                Arguments.of("10000000000000000", 1)
        );
    }

    @ParameterizedTest
    @MethodSource("providerForTotalPages")
    public void shouldReturnCorrectUsersListTotalPage(String size, int totalPagesExpected) {
        given().log().all()
                .queryParam("size", size)
                .when().get("/api/users")
                .then().log().ifValidationFails()
                .statusCode(200)
                .body("page.totalPages", is(totalPagesExpected));
    }

    private static Stream<Arguments> providerForNumber() {
        return Stream.of(
                Arguments.of("3", 3, 2),
                Arguments.of("1000", 1000, 0),
                Arguments.of("100000000000", 0, 2),
                Arguments.of("-1", 0, 2),
                Arguments.of("9", 9, 2),
                Arguments.of("10", 10, 0),
                Arguments.of("asd", 0, 2)
        );
    }

    @ParameterizedTest
    @MethodSource("providerForNumber")
    public void shouldReturnCorrectNumberOfUsersList(String page, int number, int countOfUsers) {
        JsonNode response = given().log().all()
                .queryParam("size", 2)
                .queryParam("page", page)
                .when().get("/api/users")
                .then().log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(JsonNode.class);

        assertThat(response.get("page").get("number").asInt()).isEqualTo(number);
        assertThat(response.get("_embedded").get("users").size()).isEqualTo(countOfUsers);
    }

    private static Stream<Arguments> providerSort() {
        return Stream.of(
                Arguments.of("firstName", "desc"),
                Arguments.of("firstName", "asc"),
                Arguments.of("lastName", "desc"),
                Arguments.of("lastName", "asc"),
                Arguments.of("email", "desc"),
                Arguments.of("email", "asc"),
                Arguments.of("id", "desc"),
                Arguments.of("id", "asc"),
                Arguments.of("dayOfBirth", "desc"),
                Arguments.of("dayOfBirth", "asc")
        );
    }

    @ParameterizedTest
    @MethodSource("providerSort")
    public void shouldReturnCorrectSortedUsersList(String field, String comp) {
        JsonNode response = given().log().all()
                .queryParam("sort", field + "," + comp)
                .when().get("/api/users")
                .then().log().ifValidationFails()
                .statusCode(200)
                .extract().body().as(JsonNode.class)
                .get("_embedded").get("users");

        ArrayList<User> users = new ArrayList<>();

        for (int i = 0; i < response.size(); i++) {
            StringReader reader = new StringReader(response.get(i).toString());

            ObjectMapper mapper = new ObjectMapper();
            User user = null;
            try {
                user = mapper.readValue(reader, User.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            users.add(user);
        }

        assertThat(isSorted(users, field, comp)).isEqualTo(true);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean isSorted(ArrayList<User> users, String field, String comp) {
        for (int i = 1; i < users.size(); i++) {
            Comparable cur;
            Comparable prev;
            if ("dayOfBirth".equals(field)) {
                cur = users.get(i).getDayOfBirth();
                prev = users.get(i - 1).getDayOfBirth();
            } else if ("id".equals(field)) {
                cur = users.get(i).getId();
                prev = users.get(i - 1).getId();
            } else if ("firstName".equals(field)) {
                cur = users.get(i).getFirstName();
                prev = users.get(i - 1).getFirstName();
            } else if ("lastName".equals(field)) {
                cur = users.get(i).getLastName();
                prev = users.get(i - 1).getLastName();
            } else if ("email".equals(field)) {
                cur = users.get(i).getEmail();
                prev = users.get(i - 1).getEmail();
            } else {
                return false;
            }

            if (!(("asc".equals(comp) && cur.compareTo(prev) >= 0)
                    || ("desc".equals(comp) && cur.compareTo(prev) <= 0))) {
                return false;
            }
        }
        return true;
    }

    public static class User {
        private int id;
        private String firstName;
        private String lastName;
        private String email;
        private Date dayOfBirth;
        @JsonIgnore
        private String _links;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Date getDayOfBirth() {
            return dayOfBirth;
        }

        public void setDayOfBirth(Date dayOfBirth) {
            this.dayOfBirth = dayOfBirth;
        }

        public String get_links() {
            return _links;
        }

        public void set_links(String _links) {
            this._links = _links;
        }
    }


}
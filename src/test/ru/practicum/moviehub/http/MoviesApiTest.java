package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Year;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {

    private static final String BASE =
            "http://localhost:8080";

    private static final Gson GSON = new Gson();

    private static MoviesServer server;
    private static MoviesStore store;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {

        store = new MoviesStore();

        server = new MoviesServer(store);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(
                        Duration.ofSeconds(2)
                )
                .build();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray()
            throws Exception {

        HttpResponse<String> response =
                sendGet("/movies");

        assertEquals(
                200,
                response.statusCode()
        );

        assertContentType(response);

        List<Movie> movies = GSON.fromJson(
                response.body(),
                new ListOfMoviesTypeToken().getType()
        );

        assertTrue(movies.isEmpty());
    }

    @Test
    void getMovies_returnsPreviouslyAddedMovies()
            throws Exception {

        store.add(
                new Movie(
                        "Interstellar",
                        2014
                )
        );

        store.add(
                new Movie(
                        "Arrival",
                        2016
                )
        );

        HttpResponse<String> response =
                sendGet("/movies");

        assertEquals(
                200,
                response.statusCode()
        );

        List<Movie> movies = GSON.fromJson(
                response.body(),
                new ListOfMoviesTypeToken().getType()
        );

        assertEquals(
                2,
                movies.size()
        );
    }

    @Test
    void postMovie_withCorrectData_createsMovie()
            throws Exception {

        String json = """
                {
                  "title": "Inception",
                  "year": 2010
                }
                """;

        HttpResponse<String> response =
                sendPost(
                        "/movies",
                        json,
                        "application/json"
                );

        assertEquals(
                201,
                response.statusCode()
        );

        assertContentType(response);

        Movie movie =
                GSON.fromJson(
                        response.body(),
                        Movie.class
                );

        assertEquals(
                1,
                movie.getId()
        );

        assertEquals(
                "Inception",
                movie.getTitle()
        );

        assertEquals(
                2010,
                movie.getYear()
        );
    }

    @Test
    void postMovie_withEmptyTitle_returns422()
            throws Exception {

        String json = """
                {
                  "title": "",
                  "year": 2010
                }
                """;

        HttpResponse<String> response =
                sendPost(
                        "/movies",
                        json,
                        "application/json"
                );

        assertEquals(
                422,
                response.statusCode()
        );

        assertHasError(response);
    }

    @Test
    void postMovie_withTooLongTitle_returns422()
            throws Exception {

        String title = "a".repeat(101);

        String json =
                GSON.toJson(
                        new Movie(title, 2010)
                );

        HttpResponse<String> response =
                sendPost(
                        "/movies",
                        json,
                        "application/json"
                );

        assertEquals(
                422,
                response.statusCode()
        );
    }

    @Test
    void postMovie_withWrongYear_returns422()
            throws Exception {

        int wrongYear =
                Year.now().getValue() + 2;

        String json =
                GSON.toJson(
                        new Movie(
                                "Future",
                                wrongYear
                        )
                );

        HttpResponse<String> response =
                sendPost(
                        "/movies",
                        json,
                        "application/json"
                );

        assertEquals(
                422,
                response.statusCode()
        );
    }

    @Test
    void postMovie_withWrongContentType_returns415()
            throws Exception {

        String json = """
                {
                  "title": "Inception",
                  "year": 2010
                }
                """;

        HttpResponse<String> response =
                sendPost(
                        "/movies",
                        json,
                        "text/plain"
                );

        assertEquals(
                415,
                response.statusCode()
        );
    }

    @Test
    void postMovie_withInvalidJson_returns400()
            throws Exception {

        HttpResponse<String> response =
                sendPost(
                        "/movies",
                        "{broken json",
                        "application/json"
                );

        assertEquals(
                400,
                response.statusCode()
        );
    }

    @Test
    void getMovieById_whenExists_returnsMovie()
            throws Exception {

        Movie saved =
                store.add(
                        new Movie(
                                "Dune",
                                2021
                        )
                );

        HttpResponse<String> response =
                sendGet(
                        "/movies/" + saved.getId()
                );

        assertEquals(
                200,
                response.statusCode()
        );

        Movie movie =
                GSON.fromJson(
                        response.body(),
                        Movie.class
                );

        assertEquals(
                saved.getId(),
                movie.getId()
        );
    }

    @Test
    void getMovieById_whenNotExists_returns404()
            throws Exception {

        HttpResponse<String> response =
                sendGet("/movies/999");

        assertEquals(
                404,
                response.statusCode()
        );
    }

    @Test
    void getMovieById_whenIdIsNotNumber_returns400()
            throws Exception {

        HttpResponse<String> response =
                sendGet("/movies/abc");

        assertEquals(
                400,
                response.statusCode()
        );
    }

    @Test
    void deleteMovie_whenExists_returns204()
            throws Exception {

        Movie saved =
                store.add(
                        new Movie(
                                "Alien",
                                1979
                        )
                );

        HttpResponse<String> response =
                sendDelete(
                        "/movies/" + saved.getId()
                );

        assertEquals(
                204,
                response.statusCode()
        );

        assertTrue(
                store.findById(
                        saved.getId()
                ).isEmpty()
        );
    }

    @Test
    void deleteMovie_whenNotExists_returns404()
            throws Exception {

        HttpResponse<String> response =
                sendDelete("/movies/999");

        assertEquals(
                404,
                response.statusCode()
        );
    }

    @Test
    void deleteMovie_whenIdIsNotNumber_returns400()
            throws Exception {

        HttpResponse<String> response =
                sendDelete("/movies/abc");

        assertEquals(
                400,
                response.statusCode()
        );
    }

    @Test
    void getMoviesByYear_returnsMatchingMovies()
            throws Exception {

        store.add(
                new Movie(
                        "Movie One",
                        2001
                )
        );

        store.add(
                new Movie(
                        "Movie Two",
                        2001
                )
        );

        store.add(
                new Movie(
                        "Movie Three",
                        2002
                )
        );

        HttpResponse<String> response =
                sendGet(
                        "/movies?year=2001"
                );

        assertEquals(
                200,
                response.statusCode()
        );

        List<Movie> movies = GSON.fromJson(
                response.body(),
                new ListOfMoviesTypeToken().getType()
        );

        assertEquals(
                2,
                movies.size()
        );
    }

    @Test
    void getMoviesByYear_whenNothingFound_returnsEmptyList()
            throws Exception {

        HttpResponse<String> response =
                sendGet(
                        "/movies?year=1999"
                );

        assertEquals(
                200,
                response.statusCode()
        );

        List<Movie> movies = GSON.fromJson(
                response.body(),
                new ListOfMoviesTypeToken().getType()
        );

        assertTrue(movies.isEmpty());
    }

    @Test
    void getMoviesByYear_whenYearIsNotNumber_returns400()
            throws Exception {

        HttpResponse<String> response =
                sendGet(
                        "/movies?year=abc"
                );

        assertEquals(
                400,
                response.statusCode()
        );
    }

    @Test
    void unsupportedMethod_returns405()
            throws Exception {

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        BASE + "/movies"
                                )
                        )
                        .PUT(
                                HttpRequest
                                        .BodyPublishers
                                        .noBody()
                        )
                        .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse
                                .BodyHandlers
                                .ofString(
                                        StandardCharsets.UTF_8
                                )
                );

        assertEquals(
                405,
                response.statusCode()
        );
    }

    private static HttpResponse<String> sendGet(
            String path
    ) throws Exception {

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        BASE + path
                                )
                        )
                        .GET()
                        .build();

        return client.send(
                request,
                HttpResponse
                        .BodyHandlers
                        .ofString(
                                StandardCharsets.UTF_8
                        )
        );
    }

    private static HttpResponse<String> sendPost(
            String path,
            String body,
            String contentType
    ) throws Exception {

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        BASE + path
                                )
                        )
                        .header(
                                "Content-Type",
                                contentType
                        )
                        .POST(
                                HttpRequest
                                        .BodyPublishers
                                        .ofString(
                                                body,
                                                StandardCharsets.UTF_8
                                        )
                        )
                        .build();

        return client.send(
                request,
                HttpResponse
                        .BodyHandlers
                        .ofString(
                                StandardCharsets.UTF_8
                        )
        );
    }

    private static HttpResponse<String> sendDelete(
            String path
    ) throws Exception {

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        BASE + path
                                )
                        )
                        .DELETE()
                        .build();

        return client.send(
                request,
                HttpResponse
                        .BodyHandlers
                        .ofString(
                                StandardCharsets.UTF_8
                        )
        );
    }

    private static void assertContentType(
            HttpResponse<String> response
    ) {

        String value =
                response.headers()
                        .firstValue(
                                "Content-Type"
                        )
                        .orElse("");

        assertEquals(
                "application/json; charset=UTF-8",
                value
        );
    }

    private static void assertHasError(
            HttpResponse<String> response
    ) {

        JsonObject json =
                GSON.fromJson(
                        response.body(),
                        JsonObject.class
                );

        assertTrue(
                json.has("error")
        );
    }
}
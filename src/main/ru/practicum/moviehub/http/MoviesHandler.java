package ru.practicum.moviehub.http;

import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {

    private static final int MIN_YEAR = 1888;
    private static final int MAX_TITLE_LENGTH = 100;

    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {

        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();

        if (path.equals("/movies")) {
            handleMovies(ex, method);
            return;
        }

        if (path.startsWith("/movies/")) {
            handleMovieById(ex, method, path);
            return;
        }

        sendError(ex, 404, "Ресурс не найден");
    }

    private void handleMovies(
            HttpExchange ex,
            String method
    ) throws IOException {

        if (method.equalsIgnoreCase("GET")) {
            handleGetMovies(ex);
            return;
        }

        if (method.equalsIgnoreCase("POST")) {
            handlePostMovie(ex);
            return;
        }

        sendError(
                ex,
                405,
                "Метод не поддерживается"
        );
    }

    private void handleGetMovies(HttpExchange ex) throws IOException {

        String query = ex.getRequestURI().getRawQuery();

        if (query == null || query.isBlank()) {
            sendJson(
                    ex,
                    200,
                    GSON.toJson(store.getMovies())
            );
            return;
        }

        String[] parameters = query.split("&");

        if (parameters.length != 1) {
            sendError(
                    ex,
                    400,
                    "Некорректный параметр запроса - 'year'"
            );
            return;
        }

        String[] parts = parameters[0].split("=", 2);

        if (parts.length != 2 || !parts[0].equals("year")) {
            sendError(
                    ex,
                    400,
                    "Некорректный параметр запроса - 'year'"
            );
            return;
        }

        try {
            int year = Integer.parseInt(parts[1]);

            List<Movie> movies = store.findByYear(year);

            sendJson(
                    ex,
                    200,
                    GSON.toJson(movies)
            );

        } catch (NumberFormatException e) {
            sendError(
                    ex,
                    400,
                    "Некорректный параметр запроса - 'year'"
            );
        }
    }

    private void handlePostMovie(HttpExchange ex) throws IOException {

        if (!hasCorrectContentType(ex)) {
            sendError(
                    ex,
                    415,
                    "Неподдерживаемый тип данных"
            );
            return;
        }

        String body = new String(
                ex.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );

        Movie movie;

        try {
            movie = GSON.fromJson(body, Movie.class);

            if (movie == null) {
                sendError(
                        ex,
                        400,
                        "Некорректный JSON"
                );
                return;
            }

        } catch (JsonParseException e) {
            sendError(
                    ex,
                    400,
                    "Некорректный JSON"
            );
            return;
        }

        List<String> validationErrors =
                validate(movie);

        if (!validationErrors.isEmpty()) {
            ErrorResponse errorResponse =
                    new ErrorResponse(
                            "Ошибка валидации",
                            validationErrors
                    );

            sendJson(
                    ex,
                    422,
                    GSON.toJson(errorResponse)
            );

            return;
        }

        Movie savedMovie = store.add(movie);

        sendJson(
                ex,
                201,
                GSON.toJson(savedMovie)
        );
    }

    private List<String> validate(Movie movie) {

        List<String> errors = new ArrayList<>();

        String title = movie.getTitle();

        if (title == null || title.isBlank()) {
            errors.add(
                    "название не должно быть пустым"
            );
        } else if (title.length() > MAX_TITLE_LENGTH) {
            errors.add(
                    "название не должно быть длиннее 100 символов"
            );
        }

        int maxYear =
                Year.now().getValue() + 1;

        if (movie.getYear() < MIN_YEAR
                || movie.getYear() > maxYear) {

            errors.add(
                    "год должен быть между "
                            + MIN_YEAR
                            + " и "
                            + maxYear
            );
        }

        return errors;
    }

    private boolean hasCorrectContentType(HttpExchange ex) {

        String contentType =
                ex.getRequestHeaders()
                        .getFirst("Content-Type");

        if (contentType == null) {
            return false;
        }

        String[] parts =
                contentType
                        .toLowerCase(Locale.ROOT)
                        .split(";");

        if (!parts[0].trim().equals("application/json")) {
            return false;
        }

        for (int i = 1; i < parts.length; i++) {

            String parameter = parts[i].trim();

            if (parameter.startsWith("charset=")) {

                String charset =
                        parameter
                                .substring("charset=".length())
                                .trim();

                if (!charset.equals("utf-8")) {
                    return false;
                }
            }
        }

        return true;
    }

    private void handleMovieById(
            HttpExchange ex,
            String method,
            String path
    ) throws IOException {

        String idString =
                path.substring("/movies/".length());

        int id;

        try {
            id = Integer.parseInt(idString);
        } catch (NumberFormatException e) {
            sendError(
                    ex,
                    400,
                    "Некорректный ID"
            );
            return;
        }

        if (method.equalsIgnoreCase("GET")) {
            handleGetMovieById(ex, id);
            return;
        }

        if (method.equalsIgnoreCase("DELETE")) {
            handleDeleteMovie(ex, id);
            return;
        }

        sendError(
                ex,
                405,
                "Метод не поддерживается"
        );
    }

    private void handleGetMovieById(
            HttpExchange ex,
            int id
    ) throws IOException {

        Optional<Movie> movie =
                store.findById(id);

        if (movie.isEmpty()) {
            sendError(
                    ex,
                    404,
                    "Фильм не найден"
            );
            return;
        }

        sendJson(
                ex,
                200,
                GSON.toJson(movie.get())
        );
    }

    private void handleDeleteMovie(
            HttpExchange ex,
            int id
    ) throws IOException {

        boolean deleted =
                store.deleteById(id);

        if (!deleted) {
            sendError(
                    ex,
                    404,
                    "Фильм не найден"
            );
            return;
        }

        sendNoContent(ex);
    }
}
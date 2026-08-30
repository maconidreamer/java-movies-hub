package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoviesStore {

    private final Map<Integer, Movie> movies = new LinkedHashMap<>();
    private int nextId = 1;

    public synchronized Movie add(Movie movie) {
        Movie savedMovie = new Movie(
                nextId,
                movie.getTitle(),
                movie.getYear()
        );

        movies.put(nextId, savedMovie);
        nextId++;

        return savedMovie;
    }

    public synchronized List<Movie> getMovies() {
        return new ArrayList<>(movies.values());
    }

    public synchronized Optional<Movie> findById(int id) {
        return Optional.ofNullable(movies.get(id));
    }

    public synchronized boolean deleteById(int id) {
        return movies.remove(id) != null;
    }

    public synchronized List<Movie> findByYear(int year) {
        List<Movie> result = new ArrayList<>();

        for (Movie movie : movies.values()) {
            if (movie.getYear() == year) {
                result.add(movie);
            }
        }

        return result;
    }

    public synchronized void clear() {
        movies.clear();
        nextId = 1;
    }
}
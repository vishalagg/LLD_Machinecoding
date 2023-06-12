package bookmyshow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class BookMyShowDesign {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        BookMyShow bookMyShow = BookMyShow.getInstance();

        List<Movie> movies = bookMyShow.getMovies(Location.GGN);
        System.out.println(bookMyShow.book(1, 1));
        System.out.println(bookMyShow.book(1, 1));

    }
}

class BookMyShow {

    public static BookMyShow INSTANCE = new BookMyShow();
    Map<Location, List<Movie>> movieMap;
    Map<Location, List<Theatre>> theatreMap;

    Map<Integer, Show> showMap;

    HashExecutor executor;

    private BookMyShow() {
        this.movieMap = new HashMap<>();
        this.theatreMap = new HashMap<>();
        this.showMap = new HashMap<>();
        this.executor = new HashExecutor(5);

        init();
    }

    private void init() {
        Movie movie1 = new Movie(1, "DON", 2);
        Movie movie2 = new Movie(2, "DON-2", 3);

        Theatre theatre1 = new Theatre(1, Location.GGN);
        Show show1 = new Show(1, movie1, theatre1);

        theatre1.addShow(show1);
        show1.addSeat(new Seat(1, 1, SeatCategory.PLATINUM, SeatStatus.AVAILABLE));
        show1.addSeat(new Seat(2, 2, SeatCategory.GOLD, SeatStatus.AVAILABLE));
        showMap.put(1, show1);
    }

    public static BookMyShow getInstance() {
        return INSTANCE;
    }

    public List<Movie> getMovies (Location location) {
        return movieMap.get(location);
    }

    public List<Theatre> getTheatres (Location location) {
        return theatreMap.get(location);
    }

    public BookingStatus book (int seatId, int showId) throws ExecutionException, InterruptedException {

        String key = showId + "-" + seatId;
        return executor.supplyAsync(key, () -> {
            Show show = showMap.get(showId);

            if (show!= null && show.seatMap.get(seatId).status.equals(SeatStatus.AVAILABLE)) {
                show.seatMap.get(seatId).status = SeatStatus.BOOKED;
                return BookingStatus.BOOKED;
            } else {
                return BookingStatus.FAILED_BOOKED_ALREADY;
            }
        }).get();
    }

    public Map<Theatre, List<Show>> getShowsMap(Movie movie, Location location) {
        Map<Theatre, List<Show>> shows = new HashMap<>();

        List<Theatre> theatres = theatreMap.getOrDefault(location, new ArrayList<>());

        for (Theatre theatre: theatres) {
            for (Show show: theatre.shows) {
                if (show.movie.id == movie.id) {
                    shows.putIfAbsent(theatre, new ArrayList<>());
                    shows.get(theatre).add(show);
                }
            }
        }

        return shows;
    }
}

enum BookingStatus {
    BOOKED,
    FAILED_BOOKED_ALREADY
}
class Theatre {
    int id;
    Location location;
    List<Show> shows;

    Theatre(int id, Location location) {
        this.id = id;
        this.location = location;
        this.shows = new ArrayList<>();
    }

    public void addShow(Show show) {
        shows.add(show);
    }
}

class User {}

enum Location {
    DEL,
    BLR,
    GGN
}

class Movie {
    int id;
    String name;
    int duration;

    public Movie(int id, String name, int duration) {
        this.id = id;
        this.name = name;
        this.duration = duration;
    }

}

class Show {
    int id;

    Movie movie;
    Theatre theatre;
    Map<Integer, Seat> seatMap;

    public Show(int id, Movie movie, Theatre theatre) {
        this.id = id;
        this.movie = movie;
        this.theatre = theatre;
        this.seatMap = new HashMap<>();
    }

    public void addSeat(Seat seat) {
        seatMap.put(seat.id, seat);
    }
}

enum SeatCategory {
    PLATINUM,
    GOLD,
    SILVER
}
class Seat {
    int id;
    int row;
    SeatCategory category;
    SeatStatus status;

    public Seat(int id, int row, SeatCategory category, SeatStatus status) {
        this.id = id;
        this.row = row;
        this.category = category;
        this.status = status;
    }
}

enum SeatStatus {
    BOOKED,
    AVAILABLE
}

class Payment {}

class HashExecutor {
    Executor[] executors;

    HashExecutor(int nThread) {
        this.executors = new Executor[nThread];

        for (int i=0; i<nThread; i++) {
            this.executors[i] = Executors.newSingleThreadExecutor();
        }
    }

    public CompletableFuture<BookingStatus> supplyAsync(final String key, final Supplier supplier) {
        return CompletableFuture.supplyAsync(supplier, executors[Math.abs(key.hashCode())%executors.length]);
    }
}
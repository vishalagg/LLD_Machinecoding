package vehiclerentaldesign;

import java.util.*;
import java.util.stream.Collectors;

public class VehicleRentalDesign {

    public static void main(String[] args) {
        VehicleRental vehicleRental = VehicleRental.getInstance();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 2);
        Date fromDate1 = Calendar.getInstance().getTime();
        Date toDate1 = calendar.getTime();

        Calendar calendar2 = Calendar.getInstance();
        calendar2.add(Calendar.DAY_OF_YEAR, 5);
        Calendar calendar3 = Calendar.getInstance();
        calendar3.add(Calendar.DAY_OF_YEAR, 10);
        Date fromDate2 = calendar2.getTime();
        Date toDate2 = calendar3.getTime();


        vehicleRental.book(1, 1, fromDate1, toDate1, 1); // should book
        vehicleRental.book(1, 1, fromDate2, toDate2, 2); // should book

        vehicleRental.book(1, 1, fromDate1, toDate2, 3); // shouldn't book as vehicle not available


    }
}

class VehicleRental {

    private final static VehicleRental INSTANCE = new VehicleRental();
    Map<Integer, User> userMap;
    private final StoreManager storeManager;

    private final Map<Integer, TreeMap<Date, Reservation>> reservationMap;

    private VehicleRental() {
        this.storeManager = new StoreManager();
        this.reservationMap = new HashMap<>();
        init();
    }

    private void init() {
        this.storeManager.addStore(Location.GGN, 1);
        this.storeManager.stores.get(0).vehicleManager.addVehicle(new Vehicle(1));
    }

    public static VehicleRental getInstance() {
        return INSTANCE;
    }

    public void addUser(String name) {
        User user = new User(UUID.randomUUID().hashCode(), name);
        userMap.put(user.id, user);
    }

    public List<Store> getStores(Location location) {
        return storeManager.findStores(location);
    }

    public List<Vehicle> getAvailableVehicles(int storeId) {
        return storeManager.getAvailableVehicles(storeId);
    }

    public Reservation book(int storeId, int vehicleId, Date reservedFrom, Date reservedTill, int userId) {

        if (reservationMap.containsKey(vehicleId)) {
            TreeMap<Date, Reservation> reservations = reservationMap.get(vehicleId);

            boolean canBeReserved = true;
            for (Map.Entry<Date, Reservation> entry : reservations.entrySet()) {
                Date reservedFromDate = entry.getKey();
                Reservation reservation = entry.getValue();
                Date reservedTillDate = reservation.reservedTill;

                if (reservedFrom.before(reservedTillDate) && reservedFromDate.before(reservedTill)) {
                    canBeReserved = false;
                    break;
                }
            }

            if (!canBeReserved) {
                throw new RuntimeException("Vehicle-" + vehicleId + " is already reserved on specified date");
            }
        }
        List<Vehicle> vehicles = storeManager.getAvailableVehicles(storeId);
        Vehicle requiredVehicle = vehicles.stream().
                filter(vehicle -> vehicle.id == vehicleId).findFirst().orElseGet(null);

        if (requiredVehicle == null) {
            throw new RuntimeException("Select another vehicle as the given one is not available");
        }

        Reservation reservation = new Reservation(UUID.randomUUID().hashCode());
        reservation.reserve(requiredVehicle, reservedFrom, reservedTill, userId);
        reservationMap.putIfAbsent(vehicleId, new TreeMap<>());
        reservationMap.get(vehicleId).putIfAbsent(reservedFrom, reservation);
        System.out.println("User-" + userId + " has successfully booked the vehicle-"
                + vehicleId + " from date-" + reservedFrom + " till date-" + reservedTill);
        return reservation;
    }
}

enum Location {
    DEL,
    GGN,
    BLR
}

class StoreManager {
    List<Store> stores;

    StoreManager() {
        this.stores = new ArrayList<>();
    }

    public void addStore(Location location, int storeId) {
        Store store = new Store(storeId, location);
        stores.add(store);
    }

    public List<Store> findStores(Location location) {
        return stores.stream().filter(store -> store.location == location).collect(Collectors.toList());
    }

    public List<Vehicle> getAvailableVehicles(int storeId) {
        Store requiredStore = stores.stream().filter(store -> store.id == storeId).findFirst().orElseGet(null);
        if (requiredStore == null) {
            throw new RuntimeException("Invalid store id");
        }

        return requiredStore.getAvailableVehicles();
    }
}

class Store {
    int id;
    VehicleManager vehicleManager;
    Location location;

    Store(int id, Location location) {
        this.id = id;
        this.location = location;
        this.vehicleManager = new VehicleManager(UUID.randomUUID().hashCode());
    }

    public List<Vehicle> getAvailableVehicles() {
        return vehicleManager.getAvailableVehicles();
    }

}

class VehicleManager {
    int id;
    List<Vehicle> vehicles;

    VehicleManager(int id) {
        this.id = id;
        this.vehicles = new ArrayList<>();
    }

    public void addVehicle(Vehicle vehicle) {
        vehicles.add(vehicle);
    }

    public List<Vehicle> getAvailableVehicles() {
        return this.vehicles;
    }
}

class Vehicle {
    int id;
    Vehicle(int id) {
        this.id = id;
    }
}

class Car extends Vehicle {

    Car(int id) {
        super(id);
    }
}

enum ReservationStatus {
    RESERVED,
    COMPLETED,
    IN_PROGRESS
}

class Reservation {
    int id;
    Vehicle vehicle;
    Date reservedFrom;
    Date reservedTill;
    int userId;
    Bill bill;

    Reservation(int id) {
        this.id = id;
    }

    public void reserve(Vehicle vehicle, Date reservedFrom, Date reservedTill, int userId) {
        if (reservedFrom.compareTo(reservedTill) >= 0) {
            throw new RuntimeException("Incorrect date");
        }

        this.vehicle = vehicle;
        this.reservedFrom = reservedFrom;
        this.reservedTill = reservedTill;
        this.userId = userId;
        this.bill = new Bill(UUID.randomUUID().hashCode());
    }
}

class Bill {
    int id;
    Payment payment;

    Bill(int id) {
        this.id = id;
    }
}

class Payment {
    int id;
    boolean isPaid;

    void pay() {
        this.isPaid = true;
        return;
    }
}

class User {
    int id;
    String name;

    User(int id, String name) {
        this.id = id;
        this.name = name;
    }
}

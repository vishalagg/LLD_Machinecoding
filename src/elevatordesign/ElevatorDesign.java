package elevatordesign;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Step 1: Requirement:
 *  - Move up/down
 *  - multiple passengers can go in/out at any floor
 *  - have buttons on each floor
 *
 * Step 2: Identify Objects:
 *  elevatordesign.Elevator, ExternalButtonPanel, InternalButtonPanel, elevatordesign.Request, elevatordesign.ElevatorSystemController,
 *  elevatordesign.ElevatorPickStrategy(I), ShortestTimeElevatorPickStrategyImpl, elevatordesign.ServeRequestStrategy(I),
 *  MinimumRoundServeRequestStrategy, elevatordesign.ElevatorHandler
 **/
public class ElevatorDesign {

    public static void main(String[] args) {
        ElevatorSystemController elevatorSystemController = new ElevatorSystemController(new ShortestDistanceElevatorPickStrategy());

        elevatorSystemController.addElevator(new Elevator(1));
        elevatorSystemController.addElevator(new Elevator(2));
        elevatorSystemController.addElevator(new Elevator(3));
        elevatorSystemController.addElevator(new Elevator(4));

        elevatorSystemController.requestForFloor(5, elevatorSystemController.requestForElevator(3));
        elevatorSystemController.requestForFloor(9, elevatorSystemController.requestForElevator(7));

    }
}

enum DIRECTION {
    UP,
    DOWN,
    IDLE
}

enum LOCATION {
    INSIDE,
    OUTSIDE
}

class Request {

    int currentFloor;
    int desiredFloor;
    LOCATION location;

    public Request(int currentFloor, int desiredFloor, LOCATION location) {
        this.currentFloor = currentFloor;
        this.desiredFloor = desiredFloor;
        this.location = location;
    }
}

class ElevatorSystemController {

    ElevatorPickStrategy elevatorPickStrategy;
    List<ElevatorHandler> elevatorHandlers;

    ElevatorSystemController(ElevatorPickStrategy elevatorPickStrategy) {
        this.elevatorPickStrategy = elevatorPickStrategy;
        this.elevatorHandlers = new ArrayList<>();
    }

    public ElevatorHandler requestForElevator(int floor) {
        ElevatorHandler elevatorHandler = elevatorPickStrategy.getElevator(elevatorHandlers, floor);

        System.out.println("elevatordesign.Elevator " + elevatorHandler.elevator.id + " is coming to requested " + floor + " floor");
        elevatorHandler.process(new Request(floor, floor, LOCATION.OUTSIDE));
        return elevatorHandler;
    }

    public void requestForFloor(int desiredFloor, ElevatorHandler elevatorHandler) {
        elevatorHandler.process(new Request(elevatorHandler.elevator.currentFloor, desiredFloor, LOCATION.INSIDE));
    }

    public Integer addElevator(Elevator elevator) {
        int id = elevator.id;
        this.elevatorHandlers.add(new ElevatorHandler(new ServeRequestStrategyImpl(), new Elevator(id)));
        return id;
    }
}

interface ElevatorPickStrategy {
    public ElevatorHandler getElevator(List<ElevatorHandler> elevatorHandlers, int userFloor);
}

class ShortestDistanceElevatorPickStrategy implements  ElevatorPickStrategy {

    @Override
    public ElevatorHandler getElevator(List<ElevatorHandler> elevatorHandlers, int userFloor) {
        ElevatorHandler minDistElevatorHandler = null;
        int minDist = Integer.MAX_VALUE;

        for (ElevatorHandler elevatorHandler: elevatorHandlers) {
            int dist = Math.abs(userFloor-elevatorHandler.elevator.currentFloor);
            if (!isTowardsUser(userFloor, elevatorHandler.elevator)) {
                dist += 2*elevatorHandler.getExtremeFloor();
            }
            if (dist < minDist) {
                minDist = dist;
                minDistElevatorHandler = elevatorHandler;
            }
        }
        return minDistElevatorHandler;
    }

    private boolean isTowardsUser(int userFloor, Elevator elevator) {

        if (userFloor == elevator.currentFloor || elevator.direction == DIRECTION.IDLE)
            return true;
        if (userFloor < elevator.currentFloor)
            return elevator.direction == DIRECTION.DOWN;
        return elevator.direction == DIRECTION.UP;
    }
}

class ElevatorHandler {
    ServeRequestStrategy serveRequestStrategy;
    Elevator elevator;

    ElevatorHandler(ServeRequestStrategy serveRequestStrategy, Elevator elevator) {
        this.elevator = elevator;
        this.serveRequestStrategy = serveRequestStrategy;
    }

    public void process(Request request) {
        serveRequestStrategy.serveRequest(this.elevator, request);
    }

    public int getExtremeFloor() {
        return serveRequestStrategy.getExtremeFloor(this.elevator);
    }
}

class Elevator {
    int id;
    int currentFloor;
    DIRECTION direction;

    public Elevator(int id) {
        this.id = id;
        this.currentFloor = 0;
        this.direction = DIRECTION.IDLE;
    }
}

interface ServeRequestStrategy {
    public void serveRequest(Elevator elevator, Request request);
    public int getExtremeFloor(Elevator elevator);
}

class ServeRequestStrategyImpl implements ServeRequestStrategy {

    PriorityQueue<Request> upRequests;
    PriorityQueue<Request> downRequests;

    ServeRequestStrategyImpl() {
        this.upRequests = new PriorityQueue<>((a, b) -> a.desiredFloor-b.desiredFloor);
        this.downRequests = new PriorityQueue<>((a, b) -> b.desiredFloor-a.desiredFloor);
    }
    @Override
    public void serveRequest(Elevator elevator, Request request) {
        if (elevator.currentFloor < request.desiredFloor)
            upRequests.add(request);
        else
            downRequests.add(request);

        if (elevator.direction == DIRECTION.UP || elevator.direction == DIRECTION.IDLE) {
            serveUpRequest(elevator);
            serveDownRequest(elevator);
        } else {
            serveDownRequest(elevator);
            serveUpRequest(elevator);
        }
    }

    private void serveUpRequest(Elevator elevator) {

        while (!upRequests.isEmpty()) {
            Request request = upRequests.poll();
            elevator.currentFloor = request.desiredFloor;
            System.out.println("Reached " + request.desiredFloor + " floor");
        }
        if (!downRequests.isEmpty())
            elevator.direction = DIRECTION.DOWN;
        else
            elevator.direction = DIRECTION.IDLE;
    }

    private void serveDownRequest(Elevator elevator) {
        while (!downRequests.isEmpty()) {
            Request request = downRequests.poll();
            elevator.currentFloor = request.desiredFloor;
            System.out.println("Reached " + request.desiredFloor + " floor");
        }
        if (!upRequests.isEmpty())
            elevator.direction = DIRECTION.UP;
        else
            elevator.direction = DIRECTION.IDLE;
    }

    @Override
    public int getExtremeFloor(Elevator elevator) {
        int extremeFloor = elevator.currentFloor;
        if (elevator.direction == DIRECTION.IDLE)
            return extremeFloor;

        if (elevator.direction == DIRECTION.UP) {
            PriorityQueue<Request> temp = new PriorityQueue<>(upRequests);
            while (!temp.isEmpty()) {
                extremeFloor = temp.poll().desiredFloor;
            }
        } else {
            PriorityQueue<Request> temp = new PriorityQueue<>(downRequests);
            while (!temp.isEmpty()) {
                extremeFloor = temp.poll().desiredFloor;
            }
        }
        return extremeFloor;
    }
}

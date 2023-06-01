package LLD_Games.snakesandladder;

import java.util.*;

public class SnakeAndLadderDesign {
    public static void main(String[] args) {
        Game game = new Game(4, 1, 2, 2, new RandomJumpStrategy());
        game.play();
    }
}

class Game {
    Board board;
    Dice dice;
    Queue<Player> players;

    Game(int dimension, int snakes, int ladders, int nPlayer, IJumpStrategy jumpStrategy) {
        this.board = new Board(dimension, snakes, ladders, jumpStrategy);
        dice = new Dice();
        players = new LinkedList<>();

        for (int i=0; i<nPlayer; i++) {
            players.add(new Player(i));
        }
    }

    public void play() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            Player player = getTurn();
            System.out.println("*****************************");
            System.out.println("Player-" + player.id + " is currently at " + player.position);
            System.out.println("Player-" + player.id + " press enter to play next move");
            sc.nextLine();
            int diceNumber = dice.roll();
            System.out.println("Dice rolled - " + diceNumber);
            int playerNewPosition = player.position + diceNumber;
            playerNewPosition = jumpCheck(playerNewPosition);
            player.position = playerNewPosition;
            System.out.println("Player-" + player.id + " new Position is: " + playerNewPosition);

            if(playerNewPosition >= board.cells.length * board.cells.length-1){
                System.out.println("Player-" + player.id + " is the WINNER!!!");
                break;
            }
        }
    }

    private int jumpCheck(int playerNewPosition) {
        if(playerNewPosition >= board.cells.length * board.cells.length ){
            return playerNewPosition;
        }

        Cell cell = board.getCell(playerNewPosition);
        if(cell.jump != null && cell.jump.start == playerNewPosition) {
            return cell.jump.end;
        }
        return playerNewPosition;

    }

    Player getTurn() {
        Player player = players.poll();
        players.add(player);
        return player;
    }

}

class Player {
    int id;
    int position;

    Player(int id) {
        this.id = id;
        this.position = 0;
    }
}

class Dice {
   Random random;

   Dice() {
       random = new Random();
   }

    public int roll() {
        return random.nextInt(6)+1;
    }
}
class Cell {
    int i;
    int j;
    Jump jump;

    Cell(int i, int j) {
        this.i = i;
        this.j = j;
    }
}

class Jump {
    int start;
    int end;

    Jump(int s, int e) {
        this.start = s;
        this.end = e;
    }
}

class Board {
    Cell[][] cells;

    Board(int dimension, int nSnakes, int nLadder, IJumpStrategy jumpStrategy) {
        this.cells = new Cell[dimension][dimension];

        for (int i=0; i<dimension; i++) {
            for (int j=0; j<dimension; j++) {
                this.cells[i][j] = new Cell(i, j);
            }
        }
        jumpStrategy.addJumps(cells, nSnakes, nLadder);
    }

    Cell getCell(int playerPosition) {
        int boardRow = playerPosition / cells.length;
        int boardColumn = (playerPosition % cells.length);
        return cells[boardRow][boardColumn];
    }

}

interface IJumpStrategy {
    public void addJumps(Cell[][] cells, int nSnake, int nLadder);
}

class RandomJumpStrategy implements IJumpStrategy {

    Random random;
    RandomJumpStrategy() {
        random = new Random();
    }
    @Override
    public void addJumps(Cell[][] cells, int nSnake, int nLadder) {

        while(nSnake > 0) {
            int snakeHead = random.nextInt(cells.length*cells.length);
            int snakeTail = random.nextInt(cells.length*cells.length);
            if(snakeTail >= snakeHead) {
                continue;
            }

            Jump snakeObj = new Jump(snakeHead, snakeTail);

            Cell cell = getCell(cells, snakeHead);
            cell.jump = snakeObj;

            nSnake--;
        }

        while(nLadder > 0) {
            int ladderStart = random.nextInt(cells.length*cells.length);
            int ladderEnd = random.nextInt(cells.length*cells.length);
            if(ladderStart >= ladderEnd) {
                continue;
            }

            Jump ladderObj = new Jump(ladderStart, ladderEnd);

            Cell cell = getCell(cells, ladderStart);
            cell.jump = ladderObj;

            nLadder--;
        }
    }

    Cell getCell(Cell[][] cells, int playerPosition) {
        int boardRow = playerPosition / cells.length;
        int boardColumn = (playerPosition % cells.length);
        return cells[boardRow][boardColumn];
    }
}


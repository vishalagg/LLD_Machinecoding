package LLD_Games.minesweeper;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;

public class MineSweeperDesign {

    public static void main(String[] args) {
        Game game = new Game(4, 4, 2, new RandomBombPlantStrategy());
        game.play();
    }
}

class Game {
    Board board;
    int rows;
    int cols;

    GameState gameState;

    Game(int r, int c, int nBombToPlant, IBombPlantStrategy bombPlantStrategy) {
        this.board = new Board(r, c, nBombToPlant, bombPlantStrategy);
        this.rows = r;
        this.cols = c;
        this.gameState = GameState.IN_PROGRESS;
    }

    public void play() {

        if (this.gameState != GameState.IN_PROGRESS) {
            System.out.println("Game Already Completed, Result was " + gameState);
            return;
        }

        board.printBoard();
        Scanner sc = new Scanner(System.in);
        while (this.gameState == GameState.IN_PROGRESS) {
            System.out.println("Enter row, col:");
            int r = sc.nextInt();
            int c = sc.nextInt();
            if (!inValidMove(r, c)) {
                System.out.println("Please enter valid move");
                continue;
            }
            GameState state = board.makeMove(r, c);
            board.printBoard();

            if (state == GameState.WON) {
                this.gameState = GameState.WON;
                System.out.println("WINNER!!!!");
            }
            if (state == GameState.LOST) {
                this.gameState = GameState.LOST;
                System.out.println("Better luck next time:(");
            }
        }

    }

    private boolean inValidMove(int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < cols && !board.cells[r][c].isExposed;
    }
}

class Cell {
    int row;
    int col;
    boolean isBomb;
    int number; // 0->blank, 1...8 -> number of nbrs having bombs
    boolean isExposed;

    Cell(int r, int c) {
        this.row = r;
        this.col = c;
    }

    public void setBomb() {
        this.isBomb = true;
        this.number = -1;
    }
}

enum GameState {
    IN_PROGRESS,
    WON,
    LOST
}

class Board {
    Cell[][] cells;
    int unexposedCells;

    Board(int r, int c, int nBombToPlant, IBombPlantStrategy bombPlantStrategy) {
        this.cells = new Cell[r][c];

        for (int i=0; i<r; i++) {
            for (int j=0; j<c; j++) {
                this.cells[i][j] = new Cell(i, j);
            }
        }
        bombPlantStrategy.plantBombs(cells, nBombToPlant);
        setNumbers();
        this.unexposedCells = r*c-nBombToPlant;
    }

    public GameState makeMove(int row, int col) {
        Cell cell = cells[row][col];
        GameState state = GameState.IN_PROGRESS;

        if (!cell.isExposed) {
            cell.isExposed = true;

            // 0 -> Blank
            if (cell.number == 0) {
                expandAroundBlank(cell);
            }
            unexposedCells--;
            if (unexposedCells == 0)
                state = GameState.WON;
            if (cell.isBomb)
                state = GameState.LOST;
        }

        return state;
    }

    private void setNumbers() {
        int[][] dirs = {
                {-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}
        };

        for (int i=0; i<cells.length; i++) {
            for (int j=0; j<cells[0].length; j++) {
                if (cells[i][j].isBomb) {
                    for (int[] dir: dirs) {
                        int r = i + dir[0];
                        int c = j + dir[1];

                        if (r >= 0 && r < cells.length && c >= 0 && c < cells[0].length) {
                            cells[r][c].number++;
                        }
                    }
                }
            }
        }
    }

    private void expandAroundBlank(Cell cell) {
        int[][] dirs = {
                {-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}
        };

        Queue<Cell> queue = new LinkedList<>();
        queue.add(cell);

        while (!queue.isEmpty()) {
            Cell current = queue.poll();

            for (int[] dir: dirs) {
                int r = current.row + dir[0];
                int c = current.col + dir[1];

                if (r >= 0 && r < cells.length && c >= 0 && c < cells[0].length && !cells[r][c].isExposed) {
                    Cell nbr = cells[r][c];
                    nbr.isExposed = true;
                    unexposedCells--;

                    if (nbr.number == 0)
                        queue.add(nbr);
                }
            }
        }
     }

     public void printBoard() {
        for (int i=0; i<cells.length; i++) {
            for (int j=0; j<cells[0].length; j++) {
                if (cells[i][j].isExposed)
                    System.out.print(cells[i][j].number +"    ");
                else
                    System.out.print("*(" + cells[i][j].isBomb + ")   ");
            }
            System.out.println();
        }
     }
}

interface IBombPlantStrategy {
    void plantBombs(Cell[][] cells, int nBomb);
}

class RandomBombPlantStrategy implements IBombPlantStrategy {

    @Override
    public void plantBombs(Cell[][] cells, int nBomb) {

        int rows = cells.length;
        int cols = cells[0].length;

        // Place bombs sequentially
        for (int i=0; i<nBomb; i++) {
            int r = i/cols;
            int c = (i-r*cols) % cols;
            cells[r][c].isBomb = true;
        }

        // Shuffle - Modified Fisher-Yates Shuffling
        Random random = new Random();
        int lastIndex = rows * cols - 1;
        while (lastIndex > 0) {
            int lr = lastIndex/cols;
            int lc = (lastIndex-lr*cols) % cols;
            Cell last = cells[lr][lc];

            int randIndex = random.nextInt(lastIndex);
            int rr = randIndex/cols;
            int rc = (randIndex-rr*cols) % cols;
            Cell rand = cells[rr][rc];

            //Swap
            boolean temp = last.isBomb;
            last.isBomb = rand.isBomb;
            rand.isBomb = temp;

            lastIndex--;
        }
    }
}
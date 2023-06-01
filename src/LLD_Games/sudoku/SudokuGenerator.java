package LLD_Games.sudoku;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SudokuGenerator {
    private final int size;
    private static final int EMPTY_CELL = 0;
    private final int numClues;

    SudokuGenerator(int size, int numClues) {
        this.size = size;
        this.numClues = numClues;

        if (!isPerfectSquare(size)) {
            throw new RuntimeException("Size should be a perfect square.");
        }
    }

    private boolean isPerfectSquare(int number) {
        int sqrt = (int) Math.sqrt(number);
        return sqrt * sqrt == number;
    }


    public int[][] generateBoard() {
        int[][] board = new int[size][size];

        // Fill the board with empty cells
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                board[row][col] = EMPTY_CELL;
            }
        }

        // Solve the board to create a valid Sudoku puzzle
        solveSudoku(board);

        // Remove random cells to create the partially filled puzzle
        Random random = new Random();
        int count = size * size - numClues;
        while (count > 0) {
            int row = random.nextInt(size);
            int col = random.nextInt(size);
            if (board[row][col] != EMPTY_CELL) {
                board[row][col] = EMPTY_CELL;
                count--;
            }
        }

        return board;
    }

    private boolean solveSudoku(int[][] board) {
        List<Integer> values = new ArrayList<>();
        for (int i = 1; i <= size; i++) {
            values.add(i);
        }
        shuffle(values);
        return solveSudoku(board, 0, 0, values);
    }

    private void shuffle(List<Integer> list) {
        Random random = new Random();
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            swap(list, i, j);
        }
    }

    private void swap(List<Integer> list, int i, int j) {
        int temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }

    private boolean solveSudoku(int[][] board, int row, int col, List<Integer> values) {
        if (col == size) {
            col = 0;
            row++;
            if (row == size) {
                return true; // All cells filled
            }
        }

        if (board[row][col] != EMPTY_CELL) {
            return solveSudoku(board, row, col + 1, values);
        }

        for (int num : values) {
            if (isValid(board, row, col, num)) {
                board[row][col] = num;
                if (solveSudoku(board, row, col + 1, values)) {
                    return true;
                }
                board[row][col] = EMPTY_CELL;
            }
        }

        return false;
    }

    private boolean isValid(int[][] board, int row, int col, int num) {
        // Check row
        for (int c = 0; c < size; c++) {
            if (board[row][c] == num) {
                return false;
            }
        }

        // Check column
        for (int r = 0; r < size; r++) {
            if (board[r][col] == num) {
                return false;
            }
        }

        int subgridSize = (int) Math.sqrt(size);
        int startRow = (row / subgridSize) * subgridSize;
        int startCol = (col / subgridSize) * subgridSize;

        for (int r = startRow; r < startRow + subgridSize; r++) {
            for (int c = startCol; c < startCol + subgridSize; c++) {
                if (board[r][c] == num) {
                    return false;
                }
            }
        }

        return true;
    }

    public void printBoard(int[][] board) {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                System.out.print(board[row][col] + " ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        SudokuGenerator generator = new SudokuGenerator(16, 220);
        int[][] board = generator.generateBoard();
        generator.printBoard(board);
        System.out.println("****************");
        generator.solveSudoku(board);
        generator.printBoard(board);
    }
}

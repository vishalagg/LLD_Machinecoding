package LLD_Games.tictactoe;


import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

public class TicTacToeDesign {

    public static void main(String[] args) {
        Game game = new Game(3, 2);
        game.play();
    }
}

class Game {

    TicTacToe ticTacToe;
    State gameState;
    Queue<Player> players;

    Game(int n, int nPlayer) {
        this.ticTacToe = new TicTacToe(n, nPlayer);
        this.players = new LinkedList<>();
        this.gameState = State.IN_PROGRESS;

        for (int i=0; i<nPlayer; i++) {
            this.players.add(new Player(i+1));
        }
    }


    public void play() {

        Scanner scanner = new Scanner(System.in);
        ticTacToe.print();
        Player player = null;
        while (gameState == State.IN_PROGRESS || gameState == State.INVALID_MOVE) {
            
            if (gameState != State.INVALID_MOVE)
                player = getTurn();
            System.out.println("Player-" + player.id + " enter row amd col");
            int row = scanner.nextInt();
            int col = scanner.nextInt();

            State state = ticTacToe.move(row, col, player.id);

            if (state == State.INVALID_MOVE) {
                System.out.println("invalid move");
                gameState = State.INVALID_MOVE;
                continue;
            }

            ticTacToe.print();

            if (state == State.DRAW) {
                System.out.println("Game has been draw!");
                gameState = State.DRAW;
                break;
            }

            if (state == State.WIN) {
                System.out.println("Player-" + player.id + " WON!!!");
                gameState = State.WIN;
                break;
            }

            gameState = State.IN_PROGRESS;
        }

    }

    private Player getTurn() {
        Player player = players.poll();
        players.add(player);
        return player;
    }
}

class Player {
    int id;

    Player(int id) {
        this.id = id;
    }
}

enum State {
    WIN,
    IN_PROGRESS,
    DRAW,

    INVALID_MOVE
}

class TicTacToe {

    int[][] board;
    int[][] rowSum;
    int[][] colSum;
    int[] diagonalSum;
    int[] antiDiagonalSum;
    int dimension;

    int movesPlayed;

    TicTacToe(int n, int nPlayers) {
        this.board = new int[n][n];
        this.dimension = n;
        this.rowSum = new int[n][nPlayers];
        this.colSum = new int[n][nPlayers];
        this.diagonalSum = new int[nPlayers];
        this.antiDiagonalSum = new int[nPlayers];
    }

    public State move(int row, int col, int player) {

        if (row<0 || row>= dimension || col <0 || col >= dimension || board[row][col] != 0)
            return State.INVALID_MOVE;

        rowSum[row][player - 1]++;
        colSum[col][player - 1]++;

        board[row][col] = player;

        if (row == col) {
            diagonalSum[player - 1]++;
        }

        if (col == dimension - row - 1) {
            antiDiagonalSum[player - 1]++;
        }

        movesPlayed++;

        if (rowSum[row][player - 1] == dimension
                || colSum[col][player - 1] == dimension
                || diagonalSum[player - 1] == dimension
                || antiDiagonalSum[player - 1] == dimension) {
            return State.WIN;
        }

        if (movesPlayed == dimension * dimension) {
            return State.DRAW;
        }

        return State.IN_PROGRESS;
    }

    public void print() {
        for (int i=0; i<dimension; i++) {
            for (int j=0; j<dimension; j++) {
                System.out.print(board[i][j] + "  ");
            }
            System.out.println();
        }
    }
}


package LLD_Games.snakefoodgame;

import java.util.*;

public class SnakeFood {

    public static void main(String[] args) {
        SnakeGame snakeGame = new SnakeGame(3, 2);
        System.out.println(snakeGame.move("R"));
        System.out.println(snakeGame.move("R"));
        System.out.println(snakeGame.move("R"));
        System.out.println(snakeGame.move("U"));
        System.out.println(snakeGame.move("L"));

    }
}

class SnakeGame {
    Deque<int[]> snakeConfiguration;
    Set<String> lookup;
    int foodIndex;
    int width;
    int height;

    public SnakeGame(int width, int height) {
        snakeConfiguration = new LinkedList<>();
        lookup = new HashSet<>();
        foodIndex = 0;
        this.width = width;
        this.height = height;
        snakeConfiguration.add(new int[]{0, 0});
        lookup.add(getKey(0, 0));
    }

    public int move(String direction) {
        int[] head = snakeConfiguration.getFirst();
        int headRow = head[0];
        int headCol = head[1];

        switch (direction) {
            case "U":
                headRow--;
                break;
            case "D":
                headRow++;
                break;
            case "L":
                headCol--;
                break;
            case "R":
                headCol++;
                break;
        }

        if (headRow < 0 || headRow >= height || headCol < 0 || headCol >= width ||
                (lookup.contains(getKey(headRow, headCol)) && !(headRow == snakeConfiguration.getLast()[0] && headCol == snakeConfiguration.getLast()[1]))) {
            return -1;
        }

        snakeConfiguration.addFirst(new int[]{headRow, headCol});
        lookup.add(getKey(headRow, headCol));

        if (foodIndex == 0 || (headRow == snakeConfiguration.getLast()[0] && headCol == snakeConfiguration.getLast()[1])) {
            generateFood();
        } else {
            int[] tail = snakeConfiguration.removeLast();
            lookup.remove(getKey(tail[0], tail[1]));
        }

        return snakeConfiguration.size() - 1;
    }

    private void generateFood() {
        Random random = new Random();
        int remainingFood = width * height - snakeConfiguration.size();

        while (true) {
            int row = random.nextInt(height);
            int col = random.nextInt(width);
            String key = getKey(row, col);

            if (!lookup.contains(key)) {
                snakeConfiguration.addLast(new int[]{row, col});
                lookup.add(key);
                foodIndex++;
                break;
            }
        }
    }

    private String getKey(int x, int y) {
        return x + "-" + y;
    }
}


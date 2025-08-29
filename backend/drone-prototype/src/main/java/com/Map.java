//Map Class - this class represents a grid map where drones can navigate. It initializes nodes and their neighbors, and provides methods to access nodes and random nodes.

package com;
import java.util.Random;

import org.springframework.stereotype.Component;


@Component
public class Map {
    private final int size;
    private final node[][] grid;
    private final Random rand = new Random();

    public Map(int size) {
        this.size = size;
        grid = new node[size][size];

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                grid[x][y] = new node(x, y);
            }
        }

       
        //straight and diaggonal neighbors
        int[][] directions = { {0, 1}, {1, 0}, {0, -1}, {-1, 0},
                            {1, 1}, {1, -1}, {-1, 1}, {-1, -1} };
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                node node = grid[x][y];
                for (int[] dir : directions) {
                    int nx = x + dir[0], ny = y + dir[1];
                    if (isInBounds(nx, ny)) {
                        node.neighbors.add(grid[nx][ny]);
                    }
                }
            }
        }
    }

    private boolean isInBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < size && y < size;
    }

    public node getNode(int x, int y) {
        return grid[x][y];
    }

    public node getRandomNode() {
        int x = rand.nextInt(size);
        int y = rand.nextInt(size);
        return grid[x][y];
    }

    public int getSize() {
        return size;
    }
}
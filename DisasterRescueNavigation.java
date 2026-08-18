import java.io.File;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantLock;

public class DisasterRescueNavigation {

    // The total number of locations (sectors) in the disaster zone
    private static final int totLoc = 5; 
    
    // A checkpoint that forces all 4 threads to wait until everyone finishes their tasks
    private static final CyclicBarrier phaseBarrier = new CyclicBarrier(4, new Runnable() {
        @Override
        public void run() {
            System.out.println("\n[Barrier Met] All rescue preparation threads finished calculations safely.\n");
        }
    });
    
    // Explicit Synchronization locking for mutual exclusion
    private static final ReentrantLock dataLock = new ReentrantLock();

    // Shared data variables that all threads can access and update
    private static int humanLocX = 0;
    private static int humanLocY = 0;
    private static int totEvacuationCost = 0;
    private static String finalDijkstraPath = ""; // Shared variable to pass the optimized route to the dashboard

    // Data containers that will hold the map grids loaded from the text file
    private static int coordinateBound = 100; // default value, will be overwrite from input2.txt
    private static int[][] dijkstraGraph = new int[totLoc][totLoc];
    private static int[][] primGraph = new int[totLoc][totLoc];

    public static void main(String[] args) {
        System.out.println("+=== Starting Disaster Rescue Navigation System ===+");
        
        // Load clean input values from file override
        loadInputFile("input2.txt");

        // Thread 1: Survivor Location Generator
        Thread survivorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("[Thread 1] Scanning disaster area for missing humans...");
                    Random rand = new Random();
                    
                    dataLock.lock(); 
                    try {
                        humanLocX = rand.nextInt(coordinateBound) + 1;
                        humanLocY = rand.nextInt(coordinateBound) + 1;
                        System.out.println("[Thread 1] Found survivor group location at coordinates: (" + 
                                            humanLocX + ", " + humanLocY + ")");
                    } finally {
                        dataLock.unlock(); 
                    }
                    
                    phaseBarrier.await(); 
                } catch (Exception e) { e.printStackTrace(); }
            }
        });

        // Thread 2: Dijkstra Evacuation Route Optimizer
        Thread dijkstraThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("[Thread 2] Running Dijkstra Route Optimization...");
                    
                    int[] distance = new int[totLoc];
                    boolean[] visited = new boolean[totLoc];
                    int[] parent = new int[totLoc];
                    
                    Arrays.fill(distance, Integer.MAX_VALUE);
                    Arrays.fill(parent, -1);
                    distance[0] = 0; 

                    for (int i = 0; i < totLoc - 1; i++) {
                        int u = -1;
                        for (int v = 0; v < totLoc; v++) {
                            if (!visited[v] && (u == -1 || distance[v] < distance[u])) u = v;
                        }
                        visited[u] = true;
                        for (int v = 0; v < totLoc; v++) {
                            if (dijkstraGraph[u][v] != 0 && distance[u] + dijkstraGraph[u][v] < distance[v]) {
                                distance[v] = distance[u] + dijkstraGraph[u][v];
                                parent[v] = u; 
                            }
                        }
                    }
                    
                    // Build the readable path string from the parent array
                    StringBuilder pathBuilder = new StringBuilder();
                    int currentNode = totLoc - 1; 
                    pathBuilder.insert(0, currentNode);
                    while (parent[currentNode] != -1) {
                        currentNode = parent[currentNode];
                        pathBuilder.insert(0, currentNode + " -> ");
                    }
                    
                    dataLock.lock(); 
                    try {
                        totEvacuationCost = distance[totLoc - 1];
                        finalDijkstraPath = pathBuilder.toString();
                        System.out.println("[Thread 2] Dijkstra Route Calculated -> Path Sequence: [" 
                                            + finalDijkstraPath + "] (Cost: " + totEvacuationCost + " mins)");
                    } finally {
                        dataLock.unlock();
                    }

                    phaseBarrier.await(); 
                } catch (Exception e) { e.printStackTrace(); }
            }
        });

        // Thread 3: Prim safe-network backbone planner
        Thread primThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("[Thread 3] Running Prim Network Backbone Planner...");
                    
                    int[] key = new int[totLoc];
                    boolean[] inMST = new boolean[totLoc];
                    int[] parent = new int[totLoc]; 
                    
                    Arrays.fill(key, Integer.MAX_VALUE);
                    key[0] = 0; 
                    parent[0] = -1;

                    for (int i = 0; i < totLoc; i++) {
                        int u = -1;
                        for (int v = 0; v < totLoc; v++) {
                            if (!inMST[v] && (u == -1 || key[v] < key[u])) u = v;
                        }
                        inMST[u] = true;

                        for (int v = 0; v < totLoc; v++) {
                            if (primGraph[u][v] != 0 && !inMST[v] && primGraph[u][v] < key[v]) {
                                key[v] = primGraph[u][v];
                                parent[v] = u;
                            }
                        }
                    }
                    
                    // Moving the print header line INSIDE the dataLock block keeps everything unified!
                    dataLock.lock();
                    try {
                        int totMstWeight = 0;
                        System.out.println("[Thread 3] Prim established stable communication backbone edges:");
                        for (int i = 1; i < totLoc; i++) {
                            if (parent[i] != -1) {
                                System.out.println("    -> Edge Link: Location " + parent[i] + " === (Weight: " 
                                                    + primGraph[i][parent[i]] + ") === Location " + i);
                                totMstWeight += key[i];
                            }
                        }
                        System.out.println("[Thread 3] Total network spanning tree backbone path weight: " + totMstWeight);
                    } finally {
                        dataLock.unlock();
                    }
                    
                    phaseBarrier.await(); 
                } catch (Exception e) { e.printStackTrace(); }
            }
        });

        // Thread 4: Operations Monitor Dashboard
        Thread monitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("[Thread 4] Operations dashboard initiated. Monitoring workers...");
                    
                    phaseBarrier.await(); 
                    
                    dataLock.lock(); 
                    try {
                        System.out.println("=== FINAL RESCUE MISSION LOG ===");
                        System.out.println("Target Coordinates     : (" + humanLocX + ", " + humanLocY + ")");
                        System.out.println("Optimal Route Sequence : [" + finalDijkstraPath + "]");
                        System.out.println("Total Travel Time      : " + totEvacuationCost + " minutes");
                        System.out.println("Status                 : Evacuation route clear. Dispatching teams immediately!");
                        System.out.println("=================================");
                    } finally {
                        dataLock.unlock();
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        });

        // Run all threads concurrently
        survivorThread.start();
        dijkstraThread.start();
        primThread.start();
        monitorThread.start();
    }

    // Reads custom graph values from input file.
    private static void loadInputFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("[System Warning] '" + fileName + "' missing from workspace. Graphs left at default 0 entries.\n");
            return;
        }

        try (Scanner fileScanner = new Scanner(file)) {
            while (fileScanner.hasNext()) {
                String line = fileScanner.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                coordinateBound = Integer.parseInt(line);
                break;
            }

            int row = 0;
            while (row < totLoc && fileScanner.hasNext()) {
                String line = fileScanner.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] values = line.split("\\s+");
                for (int col = 0; col < totLoc; col++) {
                    dijkstraGraph[row][col] = Integer.parseInt(values[col]);
                }
                row++;
            }

            row = 0;
            while (row < totLoc && fileScanner.hasNext()) {
                String line = fileScanner.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] values = line.split("\\s+");
                for (int col = 0; col < totLoc; col++) {
                    primGraph[row][col] = Integer.parseInt(values[col]);
                }
                row++;
            }
            System.out.println("[System Info] Input datasets successfully loaded from '" + fileName + "'.\n");

        } catch (Exception e) {
            System.out.println("[System Error] Text formatting unexpected inside data mapping blocks.\n");
        }
    }
}
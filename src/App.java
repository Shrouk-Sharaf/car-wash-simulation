import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

class Semaphore {
    private int value;
    public Semaphore(int value) {
        this.value = value;
    }

    public synchronized void waitSemaphore() throws InterruptedException {
        while (value == 0) {
            wait();
        }
        value--;
    }
    
    public synchronized void signal() {
        value++;    
        notify();     
    }

    public synchronized int availablePermits() {
        return value;
    }
}

class Car extends Thread {
    private int id;
    private Queue<Car> carQueue;
    private Semaphore mutex;
    private Semaphore availableAreas;
    private Semaphore waitingCars;
    
    public Car(int id, Queue<Car> carQueue, Semaphore mutex, Semaphore availableAreas, Semaphore waitingCars) {
        this.id = id;
        this.carQueue = carQueue;
        this.mutex = mutex;
        this.availableAreas = availableAreas;
        this.waitingCars = waitingCars;
    }

    public void run() {
        System.out.println("C" + id + " arrived");
        
        try {
            boolean hadToWait = false;
            
            mutex.waitSemaphore();
            if (availableAreas.availablePermits() == 0) {
                hadToWait = true;
            }
            mutex.signal();
            
            availableAreas.waitSemaphore();
            
            mutex.waitSemaphore();
            carQueue.add(this);
            
            if (hadToWait) {
                System.out.println("C" + id + " arrived and waiting");
            }
            
            mutex.signal();
            waitingCars.signal();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getCarId() {
        return id;
    }
}

class Pump extends Thread {
    private int id;
    private Queue<Car> carQueue;
    private Semaphore mutex;
    private Semaphore availableAreas;
    private Semaphore waitingCars;
    private Semaphore availablePumps;
    private volatile boolean running = true;

    public Pump(int id, Queue<Car> carQueue, Semaphore mutex, Semaphore availableAreas, Semaphore waitingCars, Semaphore availablePumps) {
        this.id = id;
        this.carQueue = carQueue;
        this.mutex = mutex;
        this.availableAreas = availableAreas;
        this.waitingCars = waitingCars;
        this.availablePumps = availablePumps;
    }
    
    public void stopPump() {
        running = false;
        this.interrupt();
    }
    
    public void run() {
        while (running) {
            try {
                waitingCars.waitSemaphore();    
                availablePumps.waitSemaphore();
                mutex.waitSemaphore();

                Car car = carQueue.poll();

                if (car == null) {
                    mutex.signal();
                    waitingCars.signal();  
                    availablePumps.signal();
                    continue;
                }
                
                System.out.println("Pump " + id + ": C" + car.getCarId() + " Occupied");
                mutex.signal(); 

                Thread.sleep(100);
                System.out.println("Pump " + id + ": C" + car.getCarId() + " login");

                Thread.sleep(100);
                System.out.println("Pump " + id + ": C" + car.getCarId() + " begins service at Bay " + id);

                int serviceTime = 3000 + (int)(Math.random() * 3000);
                Thread.sleep(serviceTime);

                System.out.println("Pump " + id + ": C" + car.getCarId() + " finishes service");
                System.out.println("Pump " + id + ": Bay " + id + " is now free");
                
                availableAreas.signal(); 
                availablePumps.signal(); 
                
            }
            catch (InterruptedException e) {
                if (!running) {
                    break;
                }
                Thread.currentThread().interrupt();
            }
        }
    }
}

class ServiceStation {
    public Queue<Car> carQueue;
    public int size;
    public Semaphore mutex;
    public Semaphore availableAreas;
    public Semaphore waitingCars;
    public Semaphore availablePumps; 
    private final int numPumps;
    private Pump[] pumps;

    public ServiceStation(int numPumps, int queueSize) {
        if (numPumps < 1 || numPumps > 10) {
            System.out.println("Error: Number of pumps must be between 1 and 10. Using default: 3");
            numPumps = 3;
        }
        if (queueSize < 1 || queueSize > 10) {
            System.out.println("Error: Queue size must be between 1 and 10. Using default: 5");
            queueSize = 5;
        }

        this.numPumps = numPumps;
        this.size = queueSize;
        carQueue = new ConcurrentLinkedQueue<>();
        mutex = new Semaphore(1);
        availableAreas = new Semaphore(queueSize);
        waitingCars = new Semaphore(0);
        availablePumps = new Semaphore(numPumps);
        pumps = new Pump[numPumps];
    }

    public void startSimulation(int numCars) { 
        for (int i = 0; i < numPumps; i++) {
            Pump pump = new Pump(i + 1, carQueue, mutex, availableAreas, waitingCars, availablePumps);
            pumps[i] = pump;
            pump.start();
        }
        
        int carId = 1;
        for (int i = 0; i < numCars; i++) { 
            Car car = new Car(carId++, carQueue, mutex, availableAreas, waitingCars);
            car.start();
            try {
                Thread.sleep(200);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        waitForCompletion();
        
        stopAllPumps();
    }

    private void waitForCompletion() {
        while (true) {
            try {
                Thread.sleep(1000);
                mutex.waitSemaphore();
                boolean queueEmpty = carQueue.isEmpty();
                mutex.signal();
                
                boolean allPumpsAvailable = (availablePumps.availablePermits() == numPumps);
                
                if (queueEmpty && allPumpsAvailable) {
                    break;
                }
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private void stopAllPumps() {
        for (int i = 0; i < numPumps; i++) {
            if (pumps[i] != null) {
                pumps[i].stopPump();
            }
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("All cars processed; simulation ends");
    }
}

public class App {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        
        System.out.print("Enter number of pumps (1-10): ");
        int pumps = input.nextInt();

        System.out.print("Enter queue size (1-10): ");
        int queueSize = input.nextInt();

        System.out.print("Enter number of cars: ");
        int numCars = input.nextInt();

        ServiceStation station = new ServiceStation(pumps, queueSize);
        station.startSimulation(numCars); 

        input.close();
    }
}
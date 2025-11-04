import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

class Semaphore {
    private int value;
    public Semaphore(int value) {
        this.value = value;
    }

    public synchronized void waitSemaphore() {
        while (value == 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        value--;
    }
    
    public synchronized void signal() {
        value++;    
        notify();     
    }
}

class Car extends Thread {
    private int id;
    private Queue<Car> queue;
    private Semaphore empty, full, mutex;

    public Car(int id, Queue<Car> queue, Semaphore empty, Semaphore full, Semaphore mutex) {
        this.id = id;
        this.queue = queue;
        this.empty = empty;
        this.full = full;
        this.mutex = mutex;
    }

    public int getCarId() {
        return id;
    }

    public void run(){
        try {
            System.out.println("C" + id + " arrived");
            
            empty.waitSemaphore();
            mutex.waitSemaphore();
            
            queue.add(this);
            if (queue.size() == 1) {
                System.out.println("C" + id + " entered queue");
            } else {
                System.out.println("C" + id + " arrived and waiting");
            }
            
            mutex.signal();
            full.signal();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class Pump extends Thread {
    private int id;
    private Queue<Car> carQueue;
    private Semaphore mutex;
    private Semaphore availableAreas;
    private Semaphore waitingCars;
    private Semaphore availablePumps;
    
    public Pump(int id, Queue<Car> carQueue, Semaphore mutex, Semaphore availableAreas, Semaphore waitingCars, Semaphore availablePumps) {
        this.id = id;
        this.carQueue = carQueue;
        this.mutex = mutex;
        this.availableAreas = availableAreas;
        this.waitingCars = waitingCars;
        this.availablePumps = availablePumps;
    }
    
    public void run() {
        while (true) {
            try {
                waitingCars.waitSemaphore();
                availablePumps.waitSemaphore();
                mutex.waitSemaphore();

                Car car = carQueue.poll();
                
                System.out.println("Pump " + id + ": C" + car.getCarId() + " Occupied");
                System.out.println("Pump " + id + ": C" + car.getCarId() + " login");
                System.out.println("Pump " + id + ": C" + car.getCarId() + " begins service at Bay " + id);
                
                mutex.signal();
                availableAreas.signal();
                
                int serviceTime = (int)(Math.random() * 3000 + 2000);
                Thread.sleep(serviceTime);

                System.out.println("Pump " + id + ": C" + car.getCarId() + " finishes service");
                System.out.println("Pump " + id + ": Bay " + id + " is now free");
                availablePumps.signal();
                
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                break; 
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
        size = queueSize;
        carQueue = new LinkedList<>();
        mutex = new Semaphore(1);
        availableAreas = new Semaphore(queueSize);
        waitingCars = new Semaphore(0);
        availablePumps = new Semaphore(numPumps);
        
        System.out.println("Service Station initialized with " + numPumps + " pumps and queue size " + queueSize);
    }

    public void startSimulation(int numCars) {
        System.out.println("Simulation starting...");
        
        for (int i = 1; i <= numPumps; i++) {
            Pump pump = new Pump(i, carQueue, mutex, availableAreas, waitingCars, availablePumps);
            pump.start();
        }
        
        int carId = 1;
        int totalCars = 10;
        for (int i = 1; i <= numCars; i++) {
            Car car = new Car(carId++, carQueue, availableAreas, waitingCars, mutex);
            car.start();
            try {
                Thread.sleep((int)(Math.random() * 2000 + 1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        try {
            Thread.sleep(15000);
            System.out.println("All cars processed; simulation ends");
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class App3 {
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

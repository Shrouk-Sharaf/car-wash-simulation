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
            System.out.println("Car"+id+" arrived");
            
            if (queue.size() >= 10) {
                System.out.println("Car"+id+" left - queue full!");
                return;
            }
            
            empty.waitSemaphore();
            mutex.waitSemaphore();
            
            queue.add(this);
            System.out.println("Car"+id+" entered queue. Position: " + queue.size());
            
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
                System.out.println("Queue now has " + carQueue.size() + " cars waiting");
                
                System.out.println("Pump " + id + ": Car " + car.getCarId() + " begins service");
                mutex.signal();
                availableAreas.signal();
                
                int serviceTime = (int)(Math.random() * 3000 + 2000);
                System.out.println("Pump " + id + ": Service time " + (serviceTime/1000) + "s");
                Thread.sleep(serviceTime);

                System.out.println("Pump " + id + ": Car " + car.getCarId() + " finishes service");
                availablePumps.signal();
                
            } catch (InterruptedException e) {
                e.printStackTrace();
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
    }

    public void startSimulation() {
        for (int i = 1; i <= numPumps; i++) {
            Pump pump = new Pump(i, carQueue, mutex, availableAreas, waitingCars, availablePumps);
            pump.start();
        }
        
        int carId = 1;
        for (int i = 1; i <= 30; i++) {
            Car car = new Car(carId++, carQueue, availableAreas, waitingCars, mutex);
            car.start();
            try {
                Thread.sleep((int)(Math.random() * 2000 + 1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class App3 {
    public static void main(String[] args) {
        int pumps = 2;      
        int queueSize = 5;  

        ServiceStation station = new ServiceStation(pumps, queueSize);
        station.startSimulation();
    }
}

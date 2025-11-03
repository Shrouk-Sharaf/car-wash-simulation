import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;


//Temp
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

//Temp
class Car extends Thread {
    private int id;
    public Car(int id, Queue<Car> q, Semaphore m, Semaphore e, Semaphore f) {
        this.id = id;
    }
    public void run() {
        System.out.println("Car " + id + " arrived (stub)");
    }
}

//Temp
class Pump extends Thread {
    private int id;
    public Pump(int id, Queue<Car> q, Semaphore m, Semaphore e, Semaphore f, Semaphore p) {
        this.id = id;
    }
    public void run() {
        System.out.println("Pump " + id + " ready (stub)");
    }
}

///SHAHD'S PART
class ServiceStation {

    public static Queue<Car> carQueue;
    public static int size;
    public static Semaphore mutex;
    public static Semaphore availableAreas;
    public static Semaphore waitingCars;
    public static Semaphore availablePumps; 

    private int numPumps;

    public ServiceStation(int numPumps, int queueSize) 
    {
        if (numPumps < 1 || numPumps > 10 || queueSize < 1 || queueSize > 10) {
            System.out.println("Values must be between 1 and 10.");
            return;
        }

        this.numPumps = numPumps;
        size = queueSize;
        carQueue = new LinkedList<>();
        mutex = new Semaphore(1);         
        availableAreas = new Semaphore(queueSize); 
        waitingCars = new Semaphore(0);          
        availablePumps = new Semaphore(numPumps);  
    }

    public void startSimulation() 
    {
        System.out.println("Starting Service Station Simulation");
        for (int i = 1; i <= numPumps; i++) 
        {
            Pump pump = new Pump(i, carQueue, mutex, availableAreas, waitingCars, availablePumps);
            pump.start();
        }
        int carId = 1;
        for (int i = 1; i <= 30; i++)  
        {
            Car car = new Car(carId++, carQueue, mutex, availableAreas, waitingCars);
            car.start();
            try 
            {
                Thread.sleep((int)(Math.random() * 2000 + 1000));
            } 
            catch (InterruptedException e) 
            {
                e.printStackTrace();
            }
        }
    }
    
}
class App{
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        System.out.print("Enter number of pumps (1-10): ");
        int pumps = input.nextInt();

        System.out.print("Enter queue size (1-10): ");
        int queueSize = input.nextInt();

        ServiceStation station = new ServiceStation(pumps, queueSize);
        station.startSimulation();

        input.close();
    }
}

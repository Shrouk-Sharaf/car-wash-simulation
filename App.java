import java.awt.*;
import javax.swing.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;

/* ==============================
   LOGIC SECTION
   ============================== */

class Semaphore {
    private int value;
    public Semaphore(int value) { this.value = value; }

    public synchronized void waitSemaphore() {
        while (value == 0) {
            try { wait(); } 
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
        value--;
    }

    public synchronized void signal() {
        value++;    
        notify();
    }

    public synchronized int availablePermits() { return value; }
}

class Car extends Thread {
    private int id;
    private Queue<Car> queue;
    private Semaphore availableAreas, waitingCars, mutex;

    public Car(int id, Queue<Car> queue, Semaphore availableAreas, Semaphore waitingCars, Semaphore mutex) {
        this.id = id;
        this.queue = queue;
        this.availableAreas = availableAreas;
        this.waitingCars = waitingCars;
        this.mutex = mutex;
    }

    public int getCarId() {
        return id;
    }

    public void run(){
        try {
            System.out.println("C" + id + " arrived");
            
            availableAreas.waitSemaphore();
            mutex.waitSemaphore();
            
            queue.add(this);
            if (queue.size() == 1) {
                System.out.println("C" + id + " entered queue");
            } else {
                System.out.println("C" + id + " arrived and waiting");
            }
            
            mutex.signal();
            waitingCars.signal();
            
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
                availablePumps.signal();
                continue;
            }

            System.out.println("Pump " + id + ": C" + car.getCarId() + " Occupied");
            System.out.println("Pump " + id + ": C" + car.getCarId() + " login");
            System.out.println("Pump " + id + ": C" + car.getCarId() + " begins service at Bay " + id);
                
            mutex.signal();

            // Simulate service
            int serviceTime = (int)(Math.random() * 3000 + 2000);
            Thread.sleep(serviceTime);

            System.out.println("Pump " + id + ": C" + car.getCarId() + " finishes service");
            System.out.println("Pump " + id + ": Bay " + id + " is now free");

            availableAreas.signal();   // Queue space now available
            availablePumps.signal();   // Pump free again

        } catch (InterruptedException e) {
            if (!running) break;
        }
    }
}
}


class ServiceStation {
    private Queue<Car> carQueue;
    private Semaphore mutex, availableAreas, waitingCars, availablePumps; 
    private final int numPumps;
    private Pump[] pumps;
    private SimulationView simView;
    private StatisticsPanel statsPanel;

    public ServiceStation(int numPumps, int queueSize, SimulationView simView, StatisticsPanel statsPanel) {
        this.numPumps = numPumps;
        this.simView = simView;
        this.statsPanel = statsPanel;
        carQueue = new LinkedList<>();
        mutex = new Semaphore(1);
        availableAreas = new Semaphore(queueSize);
        waitingCars = new Semaphore(0);
        availablePumps = new Semaphore(numPumps);
        pumps = new Pump[numPumps];
    }

    public void startSimulation(int numCars) {
        for (int i = 0; i < numPumps; i++) {
            pumps[i] = new Pump(i + 1, carQueue, mutex, availableAreas, waitingCars, availablePumps);
            pumps[i].start();
        }

        for (int i = 1; i <= numCars; i++) {
            Car car = new Car(i, carQueue, availableAreas, waitingCars, mutex);
            car.start();
            simView.addCarToQueue("C" + i);
            statsPanel.updateWaitingCars(simView.getQueueSize());
            try { Thread.sleep((int)(Math.random() * 2000 + 1000)); }
            catch (InterruptedException ignored) {}
        }
    }
}

/* ==============================
   GUI SECTION
   ============================== */

class PumpView extends JPanel {
    private JLabel label;
    public PumpView(int id) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        label = new JLabel("Pump " + id + " (Free)", SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
        setBackground(Color.GREEN);
    }
    public void setBusy(String carName) {
        SwingUtilities.invokeLater(() -> {
            label.setText("Busy with " + carName);
            setBackground(Color.RED);
        });
    }
    public void setFree() {
        SwingUtilities.invokeLater(() -> {
            label.setText("Free");
            setBackground(Color.GREEN);
        });
    }
}

class SimulationView extends JPanel {
    private CopyOnWriteArrayList<PumpView> pumps;
    private DefaultListModel<String> queueModel;
    private JList<String> queueList;

    public SimulationView(int numPumps) {
        setLayout(new BorderLayout(10, 10));
        JPanel pumpsPanel = new JPanel(new GridLayout(1, numPumps, 10, 10));
        pumps = new CopyOnWriteArrayList<>();
        for (int i = 1; i <= numPumps; i++) {
            PumpView pump = new PumpView(i);
            pumps.add(pump);
            pumpsPanel.add(pump);
        }
        queueModel = new DefaultListModel<>();
        queueList = new JList<>(queueModel);
        queueList.setBorder(BorderFactory.createTitledBorder("Waiting Queue"));
        add(pumpsPanel, BorderLayout.CENTER);
        add(new JScrollPane(queueList), BorderLayout.SOUTH);
    }
    public void addCarToQueue(String carName) {
        SwingUtilities.invokeLater(() -> queueModel.addElement(carName));
    }
    public void removeCarFromQueue(String carName) {
        SwingUtilities.invokeLater(() -> queueModel.removeElement(carName));
    }
    public int getQueueSize() { return queueModel.size(); }
    public void setPumpBusy(int id, String carName) { pumps.get(id-1).setBusy(carName); }
    public void setPumpFree(int id) { pumps.get(id-1).setFree(); }
}

class StatisticsPanel extends JPanel {
    private JTextArea logsArea;
    private JLabel carsServedLabel, waitingCarsLabel;
    private int carsServed = 0;

    public StatisticsPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder("Statistics & Logs"));
        JPanel info = new JPanel(new GridLayout(2, 1));
        carsServedLabel = new JLabel("Cars Served: 0");
        waitingCarsLabel = new JLabel("Waiting: 0");
        info.add(carsServedLabel);
        info.add(waitingCarsLabel);
        logsArea = new JTextArea(12, 25);
        logsArea.setEditable(false);
        add(info, BorderLayout.NORTH);
        add(new JScrollPane(logsArea), BorderLayout.CENTER);
    }

    public void logEvent(String e) {
        SwingUtilities.invokeLater(() -> logsArea.append(e + "\n"));
    }
    public void updateCarsServed(int c) {
        this.carsServed = c;
        SwingUtilities.invokeLater(() -> carsServedLabel.setText("Cars Served: " + c));
    }
    public int getCarsServed() { return carsServed; }
    public void updateWaitingCars(int c) {
        SwingUtilities.invokeLater(() -> waitingCarsLabel.setText("Waiting: " + c));
    }
}

class ControlPanel extends JPanel {
    private JTextField pumpsField, queueField, carsField;
    private JButton startButton;
    private JTextArea outputArea;
    private SimulationView simView;
    private JFrame parentFrame;

    public ControlPanel(JFrame frame) {
        this.parentFrame = frame;
        setLayout(new GridBagLayout());
        setBackground(new Color(240, 248, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(new JLabel("Service Station Simulator"), gbc);
        gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.WEST;

        gbc.gridy++; add(new JLabel("Pumps (1-10):"), gbc);
        pumpsField = new JTextField(8); gbc.gridx = 1; add(pumpsField, gbc);
        gbc.gridy++; gbc.gridx = 0; add(new JLabel("Queue Size (1-10):"), gbc);
        queueField = new JTextField(8); gbc.gridx = 1; add(queueField, gbc);
        gbc.gridy++; gbc.gridx = 0; add(new JLabel("Cars:"), gbc);
        carsField = new JTextField(8); gbc.gridx = 1; add(carsField, gbc);
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        startButton = new JButton("Start Simulation"); add(startButton, gbc);
        gbc.gridy++; outputArea = new JTextArea(8, 20);
        outputArea.setEditable(false); add(new JScrollPane(outputArea), gbc);

        startButton.addActionListener(e -> startSimulation());
    }

    private void startSimulation() {
        try {
            int pumps = Integer.parseInt(pumpsField.getText());
            int queue = Integer.parseInt(queueField.getText());
            int cars = Integer.parseInt(carsField.getText());

            simView = new SimulationView(pumps);
            StatisticsPanel stats = new StatisticsPanel();
            parentFrame.add(simView, BorderLayout.CENTER);
            parentFrame.add(stats, BorderLayout.EAST);
            parentFrame.revalidate(); parentFrame.repaint();

            ServiceStation station = new ServiceStation(pumps, queue, simView, stats);
            new Thread(() -> station.startSimulation(cars)).start();

            outputArea.append("Simulation started...\n");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter valid numbers.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

/* ==============================
   MAIN SECTION
   ============================== */

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Service Station Simulation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.add(new ControlPanel(frame), BorderLayout.WEST);
            frame.setSize(1000, 600);
            frame.setVisible(true);
        });
    }
}

# Car Wash Simulation

This project implements a classic **Producer-Consumer Problem** in Java, simulating a busy Car Wash and Gas Station with synchronized resource management. The simulation models a service station with limited service bays and a fixed-size waiting area for vehicles.

## Team Roles

| Team Member | Main Role | Responsibilities |
|-------------|-----------|------------------|
| Shahd | ServiceStation | Main class setup, resource initialization, thread management |
| Kenzy | Semaphore Class | Custom semaphore implementation with synchronization |
| Gaeza | Car Class (Producer) | Car arrival logic, queue entry, semaphore acquisition |
| Amina | Pump Class (Consumer) | Service logic, car processing, resource release |
| Shrouq| Support & Logging | Queue validation, message logging, simulation timing |

## Technical Implementation

### Core Components
1. **ServiceStation** - Main class initializing semaphores, queue, and thread pools
2. **Semaphore** - Custom implementation using `synchronized`, `wait()`, and `notify()`
3. **Car** - Producer threads representing arriving vehicles
4. **Pump** - Consumer threads representing service bays

### Synchronization Strategy
- **Bounded Buffer** pattern for the waiting queue (size: 1-10)
- **Semaphores** for controlling access to shared resources
- **Mutex** for ensuring thread-safe queue operations
- **Proper resource acquisition/release** sequencing

## Features
- Thread-safe queue management using semaphores
- Multiple service bays working concurrently
- Real-time simulation logging with detailed messages
- Configurable waiting area and pump counts
- Queue size validation (1-10 vehicles)
- Realistic service time simulation

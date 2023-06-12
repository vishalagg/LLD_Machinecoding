package practice.jobscheduler;

import java.util.Calendar;
import java.util.Date;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class JobSchedulerDesignPractice {

    public static void main(String[] args) {
        IJobScheduler scheduler = Scheduler.getInstance();

        Runnable task1 = () -> System.out.println("Task 1 - one time");
        Runnable task2 = () -> System.out.println("Task 2 - Fix Delay");
        Runnable task3 = () -> System.out.println("Task 3 - Fix Interval");

        scheduler.schedule(task1, 5000);
        scheduler.scheduleWithFixedDelay(task2, 1000, 1000);
        scheduler.scheduleAtFixedRate(task3, 5000, 1000);
    }
}

interface IJobScheduler {
    public void schedule(Runnable job, long delayMs);
    public void scheduleAtFixedRate(Runnable job, long delayMs, long recurringDelay);
    public void scheduleWithFixedDelay(Runnable job, long delayMs, long recurringDelay);
}

class Scheduler implements IJobScheduler {

    private static final Scheduler INSTANCE = new Scheduler();
    private PriorityQueue<Task> tasks;
    private Lock queueLock;
    private Condition entryAdded;

    private Scheduler() {
        this.tasks = new PriorityQueue<>((t1, t2) -> t1.startTime.compareTo(t2.startTime));
        this.queueLock = new ReentrantLock();
        this.entryAdded = queueLock.newCondition();

        new Thread(new TaskExecutor(tasks, queueLock, entryAdded)).start();
    }

    public static Scheduler getInstance() {
        return INSTANCE;
    }

    @Override
    public void schedule(Runnable job, long delayMs) {
        Task task = new Task(job, new Date(Calendar.getInstance().getTimeInMillis() + delayMs), 0, JOB_TYPE.ONCE);
        addToQueue(task);
    }

    @Override
    public void scheduleAtFixedRate(Runnable job, long delayMs, long recurringDelay) {
        Task task = new Task(job, new Date(Calendar.getInstance().getTimeInMillis() + delayMs), recurringDelay, JOB_TYPE.FIXED_RATE);
        addToQueue(task);
    }

    @Override
    public void scheduleWithFixedDelay(Runnable job, long delayMs, long recurringDelay) {
        Task task = new Task(job, new Date(Calendar.getInstance().getTimeInMillis() + delayMs), recurringDelay, JOB_TYPE.FIXED_DELAY);
        addToQueue(task);
    }

    private void addToQueue(Task task) {
        queueLock.lock();
        try {
            tasks.add(task);
            entryAdded.signalAll();
        } finally {
            queueLock.unlock();
        }
    }
}

class TaskExecutor implements Runnable {
    private PriorityQueue<Task> tasks;
    private Lock queueLock;
    private Condition entryAdded;

    private Executor executor;

    private static final int THREAD_COUNT = 3;


    public TaskExecutor(PriorityQueue<Task> tasks, Lock queueLock, Condition entryAdded) {
        this.tasks = tasks;
        this.entryAdded = entryAdded;
        this.queueLock = queueLock;
        this.executor = Executors.newFixedThreadPool(THREAD_COUNT);
    }

    @Override
    public void run() {

        while (true) {

            queueLock.lock();
            try {
                if (!tasks.isEmpty()) {
                    Task task = tasks.peek();

                    if (task.startTime.compareTo(Calendar.getInstance().getTime()) <= 0) {
                        tasks.poll();
                        executor.execute(task);
                    }
                } else {
                    try {
                        entryAdded.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            } finally {
                queueLock.unlock();
            }
        }
    }
}

class Task implements Runnable {

    int id;
    Runnable job;
    Date startTime;
    long recurringDelay;

    JOB_TYPE type;

    Task(Runnable job, Date startTime, long recurringDelay, JOB_TYPE type) {
        this.id = UUID.randomUUID().hashCode();
        this.startTime = startTime;
        this.recurringDelay = recurringDelay;
        this.type = type;
        this.job = job;
    }

    @Override
    public void run() {
        if (JOB_TYPE.FIXED_RATE.equals(type)) {
            Scheduler.getInstance().scheduleAtFixedRate(job, recurringDelay, recurringDelay);
        }
        try {
            job.run();
        } catch (Exception e) {
            System.out.println("Error while executing job-" + id);
        }

        if (JOB_TYPE.FIXED_DELAY.equals(type)) {
            Scheduler.getInstance().scheduleAtFixedRate(job, recurringDelay, recurringDelay);
        }    }
}

enum JOB_TYPE {
    ONCE,
    FIXED_RATE,
    FIXED_DELAY
}

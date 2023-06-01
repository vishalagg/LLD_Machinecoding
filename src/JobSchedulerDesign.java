import java.util.Calendar;
import java.util.Date;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/***
 * Req:
 * 1. Client should be able to schedule one time task with some delay.
 * 2. Client should be able to schedule a recurring task with fixed interval.
 * 3. Client should be able to schedule a recurring task with some initial delay.
 */
public class JobSchedulerDesign {
    public static void main(String[] args) {
        IScheduler scheduler = Scheduler.getInstance();

        Runnable task1 = () -> System.out.println("Task 1 - one time");
        Runnable task2 = () -> System.out.println("Task 2 - one time");

        scheduler.schedule(task1, 5000);
        scheduler.schedule(task2, 1000);

        Runnable task3 = () -> System.out.println("Task 3 - Fix Interval");
        scheduler.scheduledAtFixedRate(task3, 5000, 1000);
    }
}

interface IScheduler {
    public void schedule(Runnable task, long delayInMs);
    public void scheduledAtFixedRate(Runnable task, long delayInMs, long recurringDelayInMs);
    public void scheduledWithFixedDelay(Runnable task, long delayInMs, long recurringDelayInMs);
}

class Scheduler implements IScheduler {

    private static IScheduler INSTANCE;
    private final PriorityQueue<Job> jobPriorityQueue;
    private final Lock queueLock;
    private final Condition entryAdded;


    private Scheduler(int nThread) {
        this.jobPriorityQueue = new PriorityQueue<Job>();
        this.queueLock = new ReentrantLock();
        this.entryAdded = queueLock.newCondition();

        new Thread(new JobExecutor(jobPriorityQueue, queueLock, entryAdded, nThread)).start();
    }

    public static synchronized IScheduler getInstance() {
        if (INSTANCE == null)
            INSTANCE = new Scheduler(3);
        return INSTANCE;
    }

    @Override
    public void schedule(Runnable task, long delayInMs) {
        Date date = new Date(Calendar.getInstance().getTimeInMillis() + delayInMs);
        Job job = new Job(UUID.randomUUID().toString(), task, date, JobType.ONCE);
        addJobToQueue(job);
    }

    @Override
    public void scheduledAtFixedRate(Runnable task, long delayInMs, long recurringDelayInMs) {
        Date date = new Date(Calendar.getInstance().getTimeInMillis() + delayInMs);
        Job job = new Job(UUID.randomUUID().toString(), task, date, recurringDelayInMs, JobType.FIXED_RATE);
        addJobToQueue(job);
    }

    @Override
    public void scheduledWithFixedDelay(Runnable task, long delayInMs, long recurringDelayInMs) {
        Date date = new Date(Calendar.getInstance().getTimeInMillis() + delayInMs);
        Job job = new Job(UUID.randomUUID().toString(), task, date, recurringDelayInMs, JobType.FIXED_DELAY);
        addJobToQueue(job);
    }

    private void addJobToQueue(Job job) {
        queueLock.lock();
        try {
            jobPriorityQueue.add(job);
            entryAdded.signal();
        } finally {
            queueLock.unlock();
        }
    }
}

class JobExecutor implements Runnable {

    private final Executor executor;
    private final PriorityQueue<Job> jobPriorityQueue;
    private final Lock queueLock;
    private final Condition entryAdded;

    public JobExecutor(PriorityQueue<Job> jobPriorityQueue, Lock queueLock, Condition entryAdded, int nThread) {
        this.jobPriorityQueue = jobPriorityQueue;
        this.queueLock = queueLock;
        this.entryAdded = entryAdded;
        this.executor = Executors.newFixedThreadPool(nThread);
    }

    @Override
    public void run() {
        while (true) {
            queueLock.lock();
            try {
                if (!jobPriorityQueue.isEmpty()) {
                    Job job = jobPriorityQueue.peek();
                    Date statTime = job.startTime;

                    Date currentTime = Calendar.getInstance().getTime();
                    if (currentTime.compareTo(statTime) >= 0) {
                        jobPriorityQueue.remove();
                        executor.execute(job);
                    }
                } else {
                    try {
                        entryAdded.await();
                    } catch (InterruptedException e) {
                        System.out.println(e);
                    }
                }
            } finally {
                queueLock.unlock();
            }
        }
    }
}

class Job implements Runnable, Comparable<Job> {

    String jobId;
    Runnable task;
    Date startTime;
    long reschedulePeriod;
    JobType jobType;

    public Job(String jobId, Runnable task, Date startTime, long reschedulePeriod, JobType jobType) {
        this.jobId = jobId;
        this.task = task;
        this.startTime = startTime;
        this.reschedulePeriod = reschedulePeriod;
        this.jobType = jobType;
    }

    public Job(String jobId, Runnable task, Date startTime, JobType jobType) {
        this.jobId = jobId;
        this.task = task;
        this.startTime = startTime;
        this.reschedulePeriod = 0;
        this.jobType = jobType;
    }

        @Override
    public int compareTo(Job o) {
        return this.startTime.compareTo(o.startTime);
    }

    @Override
    public void run() {
        if (JobType.FIXED_RATE.equals(jobType)) {
            Scheduler.getInstance().scheduledAtFixedRate(this.task, reschedulePeriod, reschedulePeriod);
        }

        System.out.println("Executing job: " + jobType);
        try {
            task.run();
        } finally {
            if (JobType.FIXED_DELAY.equals(jobType)) {
                Scheduler.getInstance().scheduledWithFixedDelay(this.task, reschedulePeriod, reschedulePeriod);
            }
        }
    }
}

enum JobType {
    ONCE,
    FIXED_RATE,
    FIXED_DELAY
}

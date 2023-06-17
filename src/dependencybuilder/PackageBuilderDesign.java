package dependencybuilder;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PackageBuilderDesign {

    public static void main(String[] args) {
        // 3->1->2,5    4->6
        Runnable job1 = () -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Project1 | Build Package - 1");
        };
        Runnable job2 = () -> System.out.println("Project1 | Build Package - 2");

        Runnable job3 = () -> System.out.println("Project1 | Build Package - 3");
        Runnable job4 = () -> System.out.println("Project1 | Build Package - 4");

        Runnable job5 = () -> System.out.println("Project1 | Build Package - 5");
        Runnable job6 = () -> System.out.println("Project1 | Build Package - 6");

        Map<Runnable, List<Runnable>> jobs = new HashMap<>();
        jobs.putIfAbsent(job1, new ArrayList<>());
        jobs.get(job1).add(job2);
        jobs.get(job1).add(job5);

        jobs.putIfAbsent(job3, new ArrayList<>());
        jobs.get(job3).add(job1);

        jobs.putIfAbsent(job4, new ArrayList<>());
        jobs.get(job4).add(job6);

        PackageBuilder.getInstance().build(jobs, 6);

        Runnable job21 = () -> {
            try {
                Thread.sleep(2000);
                System.out.println("Project2 | Build Package - 1");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        Runnable job22 = () ->  {
            try {
                Thread.sleep(3000);
                System.out.println("Project2 | Build Package - 2");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        Map<Runnable, List<Runnable>> jobs2 = new HashMap<>();

        jobs2.putIfAbsent(job21, new ArrayList<>());
        jobs2.get(job21).add(job22);
        PackageBuilder.getInstance().build(jobs2, 2);

    }
}

class PackageBuilder {

    private static final PackageBuilder INSTANCE = new PackageBuilder();
    private final Executor executor;
    private PackageBuilder() {
        this.executor = Executors.newFixedThreadPool(10);
    }

    public static synchronized PackageBuilder getInstance() {
        return INSTANCE;
    }

    public void build(Map<Runnable, List<Runnable>> jobs, int nPackages) {

        List<Integer>[] graph = new List[nPackages];
        Map<Integer, Task> taskIndexMap = new HashMap<>();
        Map<Runnable, Task> runnableToTaskMap = new HashMap<>();

        // init the graph
        for (int i=0; i<nPackages; i++) {
            graph[i] = new ArrayList<>();
        }

        int counter = 0;
        for (Map.Entry<Runnable, List<Runnable>> entry: jobs.entrySet()) {
            Runnable key = entry.getKey();
            List<Runnable> children = entry.getValue();

            Task keyTask = null;
            if (!runnableToTaskMap.containsKey(key)) {
                keyTask = new Task(counter, key);
                runnableToTaskMap.put(key, keyTask);
                taskIndexMap.put(counter, keyTask);
                counter++;
            } else {
                keyTask = runnableToTaskMap.get(key);
            }

            for (Runnable child: children) {
                Task childTask = null;
                if (!runnableToTaskMap.containsKey(child)) {
                    childTask = new Task(counter, child);
                    runnableToTaskMap.put(child, childTask);
                    taskIndexMap.put(counter, childTask);
                    counter++;
                } else {
                    childTask = runnableToTaskMap.get(child);
                }

                graph[keyTask.id].add(childTask.id);
            }
        }

        executor.execute(() -> {
            PackageBuilderHandler builder = new PackageBuilderHandler();
            builder.addPackageGraph(graph, taskIndexMap);
        });
    }
}

class PackageBuilderHandler {

    private final Queue<Task> queue;
    private final Lock lock;
    private final Condition entryAdded;

    private List<Integer>[] graph;

    private Map<Integer, Task> taskIndexMap;

    PackageBuilderHandler() {
        this.queue = new LinkedList<>();
        this.lock = new ReentrantLock();
        this.entryAdded = lock.newCondition();
        this.graph = new List[100];
        this.taskIndexMap = new HashMap<>();
    }

    public void addPackageGraph(List<Integer>[] graph, Map<Integer, Task> taskIndexMap) {

        this.graph = graph;
        this.taskIndexMap = taskIndexMap;

        new Thread(new BuildExecutor(this.queue, this.lock, this.entryAdded, this.graph, this.taskIndexMap)).start();

        int n = taskIndexMap.size();

        int[] inDegree = new int[n];

        for (int i=0; i<n; i++) {
            for (int id: graph[i]) {
                inDegree[id]++;
            }
        }

        for (int i=0 ;i<n; i++) {
            if (inDegree[i] == 0)
                QueueUtil.addToQueue(queue, lock, entryAdded, taskIndexMap.get(i));
        }
    }
}

class QueueUtil {
    public static void addToQueue(Queue<Task> queue, Lock lock, Condition entryAdded, Task task) {
        lock.lock();
        try {
            queue.add(task);
            entryAdded.signalAll();
        } finally {
            lock.unlock();
        }
    }
}

class BuildExecutor implements Runnable {

    private final Queue<Task> queue;
    private final Lock lock;
    private final Condition entryAdded;
    private final List<Integer>[] graph;

    private final Map<Integer, Task> taskIndexMap;
    Executor executor;
    private static final int nThread = 10;

    public BuildExecutor(Queue<Task> queue, Lock lock, Condition entryAdded,
                         List<Integer>[] graph, Map<Integer, Task> taskIndexMap) {
        this.queue = queue;
        this.lock = lock;
        this.entryAdded = entryAdded;
        this.graph = graph;
        this.taskIndexMap = taskIndexMap;

        this.executor = Executors.newFixedThreadPool(nThread);
    }

    @Override
    public void run() {

        while (true) {
            lock.lock();
            try {
                if (!queue.isEmpty()) {
                    Task task = queue.poll();
                    task.init(this.queue, this.lock, this.entryAdded, this.graph, this.taskIndexMap);
                    executor.execute(task);
                } else {
                    try {
                        entryAdded.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }
}

class Task implements Runnable {
     int id;
     Runnable job;

    private Queue<Task> queue;
    private Lock lock;
    private Condition entryAdded;
    private List<Integer>[] graph;
    private Map<Integer, Task> taskIndexMap;


    Task(int id, Runnable job) {
        this.id = id;
        this.job = job;
    }

    public void init(Queue<Task> queue, Lock lock, Condition entryAdded, List<Integer>[] graph,
                     Map<Integer, Task> taskIndexMap) {
        this.queue = queue;
        this.lock = lock;
        this.entryAdded = entryAdded;
        this.graph = graph;
        this.taskIndexMap = taskIndexMap;

    }

    @Override
    public void run() {
        job.run();

        for (Integer taskId: graph[id]) {
            QueueUtil.addToQueue(queue, lock, entryAdded, taskIndexMap.get(taskId));
        }
    }
}

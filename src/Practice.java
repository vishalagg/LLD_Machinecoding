import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * 1. Thread vs Process:
 * 2. Thread life cycle
 * 3. Join() - wait for another thread to finish
 * 4. synchronization issues with multithreading
 * 5. Intrinsic Lock (Monitor) - Synchronized keyword
 * 6. wait() and notify() - can be called from inside synchronized block/function
 * 7. producer/consumer local sa code
 * 8. Volatile
 * 9. Deadlock
 * 10.Semaphores
 * 11.Mutex
 * 12.RE-entrant locks -> producer/consumer
 * 13.Executors/ Executor service :
 *      1. SingleThreadExecutor
 *      2. FixedThreadPool(n)
 *      3. newCachedThreadPool
 *      4. newScheduledThreadPool(cmd, delay, period, time.ms)
 *
 */

public class Practice {
}

class JoinExample {

    public static void main(String[] args) {
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Inside T1 thread");
                try {
                    Thread.sleep(5000);
                    System.out.println("T1 completed");

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        System.out.println("Inside Main thread");
        t1.start();

        try {
            System.out.println("Waiting for t1 to finish");
            t1.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Executing main again");

    }
}

class DaemonThreadExample {
    public static void main(String[] args) {
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("Daemon thread");
                }
            }
        });

        t1.setDaemon(true);

        t1.start();

        System.out.println("Inside Main Thread");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Executed Main Thread");
    }
}

class SyncFailure {
    public static int count =0;

    public static synchronized void increment() {
        count++;
    }


    public static void main(String[] args) {

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName());
                for (int i=0; i<1000; i++)
                    increment();
            }
        });

        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName());
                for (int i=0; i<1000; i++)
                    increment();
            }
        });

        t1.start();
        t2.start();

        try {
            System.out.println(Thread.currentThread().getName());
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println(count);
    }
}

class SynchronizedKeyword {

//    static Object object = new Object();
    public static int count = 0;

    public static int count2 = 0;

    // increment() and increment2() will execute serially
    public static synchronized void increment() {
        System.out.println("Inside increment..");
        count++;
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized static void increment2() {
            System.out.println("Inside increment2..");
            count2++;
            try {
                Thread.sleep(6000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
        }
    }

//    public static void temp() {
//        synchronized (object) {
////
//        }
//    }

    public static void main(String[] args) {

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName());
//                for (int i=0; i<1000; i++)
                    increment();
            }
        });

        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName());
//                for (int i=0; i<1000; i++)
                    increment2();
            }
        });

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println(count);
        System.out.println(count2);

    }
}

//Inter thread communication
//wait() and notify() - see local producer consumer code:

class LocalProducerConsumer {

    public void produce() throws InterruptedException {
        synchronized (this) {
            System.out.println("Producing item..");
            wait();
            System.out.println("Again in producing method..");
        }
    }

    public void consume() throws InterruptedException {
        Thread.sleep(1000);
        synchronized (this) {
            System.out.println("Consuming item..");
            notify();
            System.out.println("consumed..");
        }
    }

    public static void main(String[] args) {
        LocalProducerConsumer pc = new LocalProducerConsumer();

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    pc.produce();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    pc.consume();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

class LocalProducerConsumerWithLockOnSomeObject {

    Object object = new Object();

    public void produce() throws InterruptedException {
        synchronized (object) {
            System.out.println("Producing item..");
            object.wait();
            System.out.println("Again in producing method..");
        }
    }

    public void consume() throws InterruptedException {
        Thread.sleep(1000);
        synchronized (object) {
            System.out.println("Consuming item..");
            object.notify();
            System.out.println("consumed..");
        }
    }

    public static void main(String[] args) {
        LocalProducerConsumerWithLockOnSomeObject pc = new LocalProducerConsumerWithLockOnSomeObject();

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    pc.produce();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    pc.consume();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

//Better version of producer/ consumer

class BetterProducerConsumer {
    int MAX_ITEM = 5;
    int MIN_ITEM = 0;
    volatile int item = 0;
    List<Integer> list = new ArrayList<>();

    public void produce() throws InterruptedException {
        synchronized (this) {
            while (true) {
                if (list.size() == MAX_ITEM) {
                    System.out.println("waiting for item to be consumed");
                    wait();
                } else {
                    System.out.println("Producing item.." + item);
                    list.add(item);
                    item++;
                    notify();
                }
            }
        }
    }

    public void consumer() throws InterruptedException {

        synchronized (this) {
            while (true) {
                if (list.size() == MIN_ITEM) {
                    System.out.println("Waiting for producer..");
                    wait();
                } else {
                    System.out.println("consumed item : "+ list.remove(list.size()-1));
                    notify();
                }
            }
        }
    }

    public static void main(String[] args) {
        BetterProducerConsumer pc = new BetterProducerConsumer();

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    pc.produce();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    pc.consumer();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

// Deadlock code

class Deadlock {

    final Object lock1 = new Object();
    final Object lock2 = new Object();

    public void execute1() throws InterruptedException {
        synchronized (lock1) {
            Thread.sleep(100);
            System.out.println("Thread1 waiting for thread2");
            synchronized (lock2) {
                System.out.println("inside one");
            }
        }
    }

    public void execute2() throws InterruptedException {
        synchronized (lock2) {
            Thread.sleep(100);
            System.out.println("Thread2 waiting for thread1");
            synchronized (lock1) {
                System.out.println("inside two");
            }
        }
    }

    public static void main(String[] args) {
        Deadlock deadlock = new Deadlock();

        Thread t1 = new Thread(() -> {
            try {
                deadlock.execute1();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                deadlock.execute2();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("will never gets printed");
    }
}


// Semaphore

enum SocketController {
    INSTANCE;

    Semaphore semaphore2 = new Semaphore(5, true);

    public void execute() {
        try {
            System.out.println("inside Execute");
            semaphore2.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                Thread.sleep(2000);
                semaphore2.release();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

class Semaphore2 {

    int permits;
    int currentCount;

    Semaphore2 (int permits) {
        this.permits = permits;
        this.currentCount = 0;
    }

    public void acquire() throws InterruptedException {

        synchronized (this) {
            while (permits == currentCount)
                wait();

            System.out.println("Permit to " + Thread.currentThread().getId() + " currentCount = " + currentCount);
            currentCount++;
            notifyAll();
        }
    }

    public void release() throws InterruptedException {
        synchronized (this) {
            while (0 == currentCount)
                wait();

            System.out.println("Released by " + Thread.currentThread().getId() + " currentCount = " + currentCount);
            currentCount--;
            notifyAll();
        }
    }

    public static void main(String[] args) {

        ExecutorService service = Executors.newCachedThreadPool();

        for (int i=0; i<20; i++) {
            service.execute(SocketController.INSTANCE::execute);
        }
    }
}


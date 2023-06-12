package practice.messagequeue;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageQueueDesign {

    public static void main(String[] args) throws InterruptedException {
        MessageQueue mq = MessageQueue.getInstance();

        String topic1 = mq.addTopic("topic1");

        String subscriber = mq.addSubscriber("s1");
        mq.subscribe(topic1, subscriber);
        Event event1 = new Event("event1", "event1");

        Event event2 = new Event("event2", "event2");
        mq.publish(topic1, event1);
        mq.publish(topic1, event2);

        Thread.sleep(1000);

        mq.replay(topic1, subscriber, 0);

    }

}

class MessageQueue {

    private static final MessageQueue INSTANCE = new MessageQueue();
    private final Map<String, Topic> topicMap;
    private final Map<String, ISubscriber> subscriberMap;
    private final HashExecutor producerExecutor;
    private final HashExecutor consumerExecutor;

    private static final int PRODUCER_THREAD_COUNT = 3;
    private static final int CONSUMER_THREAD_COUNT = 5;


    private MessageQueue() {
        this.topicMap = new ConcurrentHashMap<>();
        this.subscriberMap = new ConcurrentHashMap<>();
        this.producerExecutor = new HashExecutor(PRODUCER_THREAD_COUNT);
        this.consumerExecutor = new HashExecutor(CONSUMER_THREAD_COUNT);
    }

    public static synchronized MessageQueue getInstance() {
        return INSTANCE;
    }

    public String addTopic(String topicName) {
        Topic topic = new Topic(UUID.randomUUID().toString(), topicName);
        topicMap.put(topic.id, topic);
        return topic.id;
    }

    public String addSubscriber(final String name) {
        ISubscriber subscriber = new TestSubscriber(UUID.randomUUID().toString(), name);
        subscriberMap.put(subscriber.getId(), subscriber);
        return subscriber.getId();
    }

    public void subscribe(final String topicId, final String subscriberId) {
        if (!topicMap.containsKey(topicId) || !subscriberMap.containsKey(subscriberId))
            throw new RuntimeException("Invalid input");
        Subscription subscription = new Subscription(subscriberMap.get(subscriberId));
        topicMap.get(topicId).subscriptions.add(subscription);
    }

    public CompletableFuture<Void> publish(final String topicId, final Event event) {
        if (!topicMap.containsKey(topicId))
            throw new RuntimeException("Invalid topicId");

        return producerExecutor.runAsync(topicId, () -> {
            topicMap.get(topicId).events.add(event);
        }).thenRun(() -> {
            topicMap.get(topicId).subscriptions.forEach(subscription -> {
                consumerExecutor.runAsync(topicId+"-"+subscription.subscriber.getId(),
                        () -> {
                            subscription.offset.getAndAdd(1);
                            subscription.subscriber.consume(event);
                        });
            });
        });
    }

    public void replay(final String topicId, final String subscriberId, final int offset) {
        if (!topicMap.containsKey(topicId) || !subscriberMap.containsKey(subscriberId))
            throw new RuntimeException("Invalid input");

        Subscription subscription = topicMap.get(topicId).subscriptions.stream()
                .filter(sub -> Objects.equals(sub.subscriber.getId(), subscriberMap.get(subscriberId).getId()))
                .findFirst().orElseGet(null);

        if (subscription == null || subscription.offset.get() < offset)
            return;
        subscription.offset.set(offset);
        System.out.println("Replaying events:");
        topicMap.get(topicId).events.subList(offset, topicMap.get(topicId).events.size()).forEach(
                event -> {
                    consumerExecutor.runAsync(subscription.subscriber.getId() +"-"+ topicId, () -> {
                        subscription.offset.getAndAdd(1);
                        subscription.subscriber.consume(event);
                    });
                }
        );
    }
}

class Topic {
    String id;
    String name;
    List<Event> events;
    List<Subscription> subscriptions;

    Topic(final String id, final String name) {
        this.id = id;
        this.name = name;
        this.events = new CopyOnWriteArrayList<>();
        this.subscriptions = new CopyOnWriteArrayList<>();
    }
}

interface ISubscriber {
    String getId();
    void consume(final Event event);
}

class TestSubscriber implements ISubscriber {

    private String id;
    private String name;

    TestSubscriber(final String id, final String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void consume(Event event) {
        System.out.println("Consumer-" + this.name + " consuming event-" + event.id);
    }
}

class Event {
    String id;
    String content;

    Event(final String id, final String content) {
        this.id = id;
        this.content = content;
    }
}

class Subscription {
    AtomicInteger offset;
    ISubscriber subscriber;

    Subscription(final ISubscriber subscriber) {
        this.subscriber = subscriber;
        this.offset = new AtomicInteger(0);
    }
}

class HashExecutor {
    Executor[] executors;

    HashExecutor(int nThread) {
        this.executors = new Executor[nThread];

        for (int i=0; i<nThread; i++) {
            executors[i] = Executors.newSingleThreadExecutor();
        }
    }

    public CompletableFuture<Void> runAsync(final String key, final Runnable runnable) {

        return CompletableFuture.runAsync(runnable, executors[Math.abs(key.hashCode())% executors.length]);
    }
}

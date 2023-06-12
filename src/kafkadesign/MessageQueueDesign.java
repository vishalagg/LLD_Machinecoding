package kafkadesign;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageQueueDesign {

    public static void main(String[] args) throws InterruptedException {
        MessageQueue mq = new MessageQueue();

        String topic1 = mq.addTopic("topic1");

        String subscriber = mq.addSubscriber("s1");
        mq.subscribe(topic1, subscriber);
        Event event1 = new Event("event1", "event1");

        Event event2 = new Event("event2", "event2");
        mq.publish(topic1, event1);
        mq.publish(topic1, event2);

        String topic2 = mq.addTopic("topic2");
        mq.subscribe(topic2, subscriber);
        Event event3 = new Event("event3", "event3");
        mq.publish(topic2, event3);

        Thread.sleep(2000);
        System.out.println("Replaying events");
        mq.replay(topic1, subscriber, 0);
    }
}

class MessageQueue {
    private Map<String, Topic> topicMap;
    private Map<String, ISubscriber> subscriberMap;
    private HashBasedExecutor producerExecutor;
    private HashBasedExecutor consumerExecutor;

    MessageQueue() {
        topicMap = new ConcurrentHashMap<>();
        subscriberMap = new ConcurrentHashMap<>();
        producerExecutor = new HashBasedExecutor(3);
        consumerExecutor = new HashBasedExecutor(3);
    }

    public String addTopic(final String topicName) {
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
                consumerExecutor.runAsync(topicId+subscription.subscriber.getId(),
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
        topicMap.get(topicId).events.subList(offset, topicMap.get(topicId).events.size()).forEach(
                event -> {
                    consumerExecutor.runAsync(subscription.subscriber.getId() + topicId, () -> {
                        subscription.offset.getAndAdd(1);
                        subscription.subscriber.consume(event);
                    });
                }
        );

    }
}

class Event {
    String id;
    Long createdAt;
    String value;

    Event(String id, String value) {
        this.id = id;
        this.createdAt = System.currentTimeMillis();
        this.value = value;
    }
}

class Topic {
    String id;

    String name;
    List<Event> events;
    List<Subscription> subscriptions;

    Topic(String id, String name) {
        this.id = id;
        this.name = name;
        events = new CopyOnWriteArrayList<>();
        subscriptions = new CopyOnWriteArrayList<>();
    }
}

class Subscription {
    AtomicInteger offset;
    ISubscriber subscriber;

    Subscription(ISubscriber subscriber) {
        this.offset = new AtomicInteger(0);
        this.subscriber = subscriber;
    }
}

interface ISubscriber {
    String getId();
    void consume(Event event);
}

class TestSubscriber implements ISubscriber {

    String id;
    String name;

    TestSubscriber(final String id, final String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void consume(Event event) {
        System.out.println("Consumer with name = " + name + " consumed eventId = " + event.id);
    }
}

class HashBasedExecutor {
    private ExecutorService[] executorServices;

    HashBasedExecutor(final int nThreads) {
        executorServices = new ExecutorService[nThreads];
        for (int i=0; i<executorServices.length; i++) {
            executorServices[i] = Executors.newSingleThreadExecutor();
        }
    }

    public CompletableFuture<Void> runAsync(final String key, final Runnable runnable) {
        return CompletableFuture.runAsync(runnable, executorServices[Math.abs(key.hashCode())%executorServices.length]);
    }
}

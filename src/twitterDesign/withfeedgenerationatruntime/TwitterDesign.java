package twitterDesign.withfeedgenerationatruntime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.PriorityBlockingQueue;

public class TwitterDesign {
    public static void main(String[] args) {
        TwitterWithFeedGenerationAtRunTime twitter = TwitterWithFeedGenerationAtRunTime.getInstance();

        twitter.postTweet(1,  "user1 - tweet1.1");
        twitter.postTweet(2,  "user2 - tweet2.1");
        twitter.postTweet(1,  "user1 - tweet1.3");
        twitter.postTweet(2,  "user2 - tweet2.4");
        twitter.postTweet(3,  "user3 - tweet3.5");

        twitter.follow(1, 2);
        twitter.follow(2, 1);

        System.out.println("User 1's News Feed: ");
        for (Tweet tweet: twitter.getNewsFeed(1)) {
            System.out.println(tweet.content);
        }
        System.out.println("**************************");

        System.out.println("User 2's News Feed: ");
        for (Tweet tweet: twitter.getNewsFeed(2)) {
            System.out.println(tweet.content);
        }
        twitter.unfollow(  1, 2);
        twitter.unfollow(  2, 1);
        System.out.println("************UNFOLLOWED**************");

        System.out.println("User 1's News Feed: ");
        for (Tweet tweet: twitter.getNewsFeed(1)) {
            System.out.println(tweet.content);
        }
        System.out.println("**************************");

        System.out.println("User 2's News Feed: ");
        for (Tweet tweet: twitter.getNewsFeed(2)) {
            System.out.println(tweet.content);
        }
    }
}

class Tweet {
    int tweetId;
    Long timestamp;
    String content;

    Tweet(int tweetId, Long timestamp, String content) {
        this.tweetId = tweetId;
        this.timestamp = timestamp;
        this.content = content;
    }
}

class TwitterWithFeedGenerationAtRunTime {
    private static final TwitterWithFeedGenerationAtRunTime INSTANCE = new TwitterWithFeedGenerationAtRunTime();
    private final Map<Integer, ConcurrentSkipListSet<Integer>> followers;
    private final Map<Integer, ConcurrentSkipListSet<Integer>> followees;

    private final Map<Integer, ConcurrentSkipListMap<Long, Integer>> userToTweetIds; // userId -> {ts -> tweetId}

    private final Map<Integer, Tweet> tweets;

    private static final Integer NEWS_FEED_CAPACITY = 10;

    private TwitterWithFeedGenerationAtRunTime() {
        tweets = new ConcurrentHashMap<>();
        followers = new ConcurrentHashMap<>();
        followees = new ConcurrentHashMap<>();
        userToTweetIds = new ConcurrentHashMap<>();
    }

    public static TwitterWithFeedGenerationAtRunTime getInstance() {
        return INSTANCE;
    }

    public void postTweet(int userId, String content) {
        userToTweetIds.putIfAbsent(userId, new ConcurrentSkipListMap<>());
        Tweet tweet = new Tweet(UUID.randomUUID().hashCode(), System.nanoTime(), content);

        userToTweetIds.get(userId).put(tweet.timestamp, tweet.tweetId);
        tweets.put(tweet.tweetId, tweet);
    }

    public List<Tweet> getNewsFeed(int userId) {
        PriorityBlockingQueue<Tweet> newsFeed = new PriorityBlockingQueue<Tweet>(NEWS_FEED_CAPACITY, (t1, t2) -> Long.compare(t2.timestamp, t1.timestamp));

        newsFeedHelper(newsFeed, userId);

        for (Integer followeeId : followees.getOrDefault(userId, new ConcurrentSkipListSet<>())) {
            newsFeedHelper(newsFeed, followeeId);
        }

        return new ArrayList<>(newsFeed);
    }

    private void newsFeedHelper(PriorityBlockingQueue<Tweet> newsFeed, int userId) {

        int counter = 0;
        for (Map.Entry<Long, Integer> entry: userToTweetIds.getOrDefault(userId, new ConcurrentSkipListMap<>()).entrySet()) {
            newsFeed.put(tweets.get(entry.getValue()));

            if (newsFeed.size() > NEWS_FEED_CAPACITY)
                newsFeed.poll();
            if (counter >= NEWS_FEED_CAPACITY)
                break;
            counter++;
        }
    }

    public void follow(int followerId, int followeeId) {
        if (followerId == followeeId)
            return;

        followers.putIfAbsent(followeeId, new ConcurrentSkipListSet<>());
        followees.putIfAbsent(followerId, new ConcurrentSkipListSet<>());
        followers.get(followeeId).add(followerId);
        followees.get(followerId).add(followeeId);
    }

    public void unfollow(int followerId, int followeeId) {
        if (followerId == followeeId)
            return;
        followers.getOrDefault(followeeId, new ConcurrentSkipListSet<>()).remove(followerId);
        followees.getOrDefault(followerId, new ConcurrentSkipListSet<>()).remove(followeeId);
    }

}

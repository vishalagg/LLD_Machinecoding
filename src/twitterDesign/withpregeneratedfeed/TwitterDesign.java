package twitterDesign.withpregeneratedfeed;

import java.util.*;
import java.util.concurrent.*;

public class TwitterDesign {
    public static void main(String[] args) {
        TwitterWithPreGeneratedNewsFeed twitter = TwitterWithPreGeneratedNewsFeed.getInstance();

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

class TwitterWithPreGeneratedNewsFeed {

    private static final TwitterWithPreGeneratedNewsFeed INSTANCE = new TwitterWithPreGeneratedNewsFeed();
    private final Map<Integer, ConcurrentSkipListSet<Integer>> followers;
    private final Map<Integer, ConcurrentSkipListSet<Integer>> followees;

    private final Map<Integer, ConcurrentSkipListMap<Long, Integer>> userToTweetIds; // userId -> {ts -> tweetId}
    private final Map<Integer, PriorityBlockingQueue<Integer>> newsFeeds;

    private final Map<Integer, ConcurrentSkipListSet<Integer>> uniqueNewsFeed;

    private final Map<Integer, Tweet> tweets;

    private static final Integer NEWS_FEED_CAPACITY = 10;

    private TwitterWithPreGeneratedNewsFeed() {
        tweets = new ConcurrentHashMap<>();
        followers = new ConcurrentHashMap<>();
        followees = new ConcurrentHashMap<>();
        userToTweetIds = new ConcurrentHashMap<>();
        newsFeeds = new ConcurrentHashMap<>();
        uniqueNewsFeed = new ConcurrentHashMap<>();
    }

    public static TwitterWithPreGeneratedNewsFeed getInstance() {
        return INSTANCE;
    }

    public Integer postTweet(int userId, String content) {
        Tweet tweet = new Tweet(UUID.randomUUID().hashCode(), System.nanoTime(), content);
        userToTweetIds.putIfAbsent(userId, new ConcurrentSkipListMap<>());
        userToTweetIds.get(userId).put(tweet.timestamp, tweet.tweetId);
        tweets.put(tweet.tweetId, tweet);
        updateNewsFeed(userId, tweet.tweetId);
        return tweet.tweetId;
    }

    public List<Tweet> getNewsFeed(int userId) {
        List<Tweet> feed = new ArrayList<>();

        if (newsFeeds.containsKey(userId)) {
            for (Integer tweetId: newsFeeds.get(userId)) {
                feed.add(tweets.get(tweetId));
            }
        }
        return feed;
    }

    public void follow(int followerId, int followeeId) {
        if (followerId == followeeId || followers.containsKey(followeeId))
            return;

        followers.putIfAbsent(followeeId, new ConcurrentSkipListSet<>());
        followees.putIfAbsent(followerId, new ConcurrentSkipListSet<>());
        followers.get(followeeId).add(followerId);
        followees.get(followerId).add(followeeId);

        for (Integer followerTweet: getCurrentNewsFeed(followeeId)) {
            updateNewsFeedHelper(followerId, followerTweet);
        }
    }

    public void unfollow(int followerId, int followeeId) {
        if (followerId == followeeId)
            return;

        if (followers.containsKey(followeeId)) {
            if (followers.get(followeeId).remove(followerId) && followees.get(followerId).remove(followeeId)) {
                newsFeeds.getOrDefault(followerId, new PriorityBlockingQueue<>()).clear();
                uniqueNewsFeed.getOrDefault(followerId, new ConcurrentSkipListSet<>()).clear();
                buildNewsFeed(followerId);
            }
        }
    }

    private void buildNewsFeed(int userId) {

        int counter = 0;
        for (Map.Entry<Long, Integer> ownTweet : userToTweetIds.getOrDefault(userId, new ConcurrentSkipListMap<>()).entrySet()) {
            if (counter > NEWS_FEED_CAPACITY)
                break;
            updateNewsFeedHelper(userId, ownTweet.getValue());
            counter++;
        }

        for (Integer followee: followees.getOrDefault(userId, new ConcurrentSkipListSet<>())) {
            counter = 0;
            for (Map.Entry<Long, Integer> tweetEntry: userToTweetIds.getOrDefault(followee, new ConcurrentSkipListMap<>()).entrySet()) {
                if (counter > NEWS_FEED_CAPACITY)
                    break;
                updateNewsFeedHelper(userId, tweetEntry.getValue());
                counter++;
            }
        }
    }

    private void updateNewsFeed(int userId, int tweetId) {
        updateNewsFeedHelper(userId, tweetId);

        for (Integer follower: followers.getOrDefault(userId, new ConcurrentSkipListSet<>())) {
            updateNewsFeedHelper(follower, tweetId);
        }
    }

    private void updateNewsFeedHelper(int userId, int tweetId) {
        PriorityBlockingQueue<Integer> newsFeed = getCurrentNewsFeed(userId);
        if (!uniqueNewsFeed.get(userId).contains(tweetId)) {
            newsFeed.add(tweetId);
            uniqueNewsFeed.get(userId).add(tweetId);
        }
        if (newsFeed.size() > NEWS_FEED_CAPACITY) {
            newsFeed.poll();
        }
    }

    private PriorityBlockingQueue<Integer> getCurrentNewsFeed(int userId) {
        newsFeeds.putIfAbsent(userId, new PriorityBlockingQueue<Integer>(NEWS_FEED_CAPACITY,
                (i1, i2) -> Long.compare(tweets.get(i2).timestamp, tweets.get(i1).timestamp)));
        uniqueNewsFeed.putIfAbsent(userId, new ConcurrentSkipListSet<>());
        return newsFeeds.get(userId);
    }
}

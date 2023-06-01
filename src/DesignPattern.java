import java.util.ArrayList;
import java.util.List;

/**
 * Creational DP:
 * 1. Singleton**
 * 2. Builder**
 * 3. Factory/Abstract factory**
 * 4. Prototype
 *
 * Structural DP:
 * 5. Adaptor
 * 6. Flyweight
 * 7. Decorator/ Wrapper
 *
 * Behavioral DP:
 * 1. Observer**
 * 2. Strategy**
 */

/**
 * Problem: define a CAT object, cat can be of multiple types: ex: LION, TIGER, HouseCAT...
 *
 * LION can roar, hunt
 * TIGER can hunt, cry
 * HouseCAT can cry, drinkMilk
 */
public class DesignPattern {
}

/**
 * Singleton using class
 */
class Singleton {
    private static Singleton singleton;

    private Singleton() {}

    public static synchronized Singleton getInstance() {
        if (singleton == null) {
            singleton = new Singleton();
        }
        return singleton;
    }
}

/**
 * Singleton using ENUM
 */
enum SingletonUsingEnum {
    INSTANCE;

    public void doSomething() {
        return;
    }
}

/**
 * Builder
 */
class EmployeeBuilder {
    private String name;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private EmployeeBuilder employee;

        public Builder setName(String name) {
            this.employee.name = name;
            return this;
        }

        public EmployeeBuilder build() {
            return this.employee;
        }

    }
}

class BuilderClient {
    public static void main(String[] args) {
        EmployeeBuilder emp = EmployeeBuilder.builder()
                .setName("abc")
                .build();
    }
}

/**
 * Observer
 */
//class Bitcoin {
//    private int price;
//
//    public void setPrice(int price) {
//        this.price = price;
//
////        sendPhoneNotifications(price);
////        sendEmailNotifications(price);
//        sendNotifications(price);
//    }
//}
//
//class Notification {
//    List<User> users = new ArrayList<>();
//
//    public void sendNotifications(int price) {
//        for (User user: users) {
//            if (user.notificationType.equals("EMAIL")) {
//                notifyViaEmail(price, user.userId);
//            }
//            if (user.notificationType.equals("Phone")) {
//                notifyViaEmail(price, user.userId);
//            }
//        }
//    }
//}
//
//class User {
//    String userId;
//    String notificationType;
//}
//
////--------
//class Bitcoin2 {
//    private int price;
//    List<BitcoinObserver> obj;
//
//    public void setPrice(int price) {
//        this.price = price;
//
//        for (BitcoinObserver obs: obj) {
//            obj.notify(userId, price);
//        }
//    }
//
//    public void addObserver(String userId, String notificationType) {
//
//        obj.add(ObsFactory.getObserver(notificationType));
//    }
//}
//
//class ObsFactory {
//
//    public static BitcoinObserver getObserver(String notificationType) {
//        return new EmailNotification();
//    }
//}
//
//interface BitcoinObserver {
//    void notify(String userId, int price);
//}
//
//class EmailNotification implements BitcoinObserver {
//
//    @Override
//    public void notify(String userId, int price) {
//        //
//    }
//}
//
//class PhoneNotification implements BitcoinObserver {
//
//    @Override
//    public void notify(String userId, int price) {
//        //
//    }
//}
import java.util.HashMap;
import java.util.Map;

/***
 * There is a deployment package version management system. A package version can be compatible with the previous version if there is no data migration required for the deployment. Implement following two methods :
 *
 * void addVersion(long versionnumber, bool isCompatibleWithPrev)
 * bool isCompatible(long version1, long version2)
 * Write well structured and modular code.
 *
 */
public class VersionManagerDesign {
    public static void main(String[] args) {
        VersionManager versionManager = new VersionManager();

        versionManager.addVersion(1L, true);
        versionManager.addVersion(2L, true);
        versionManager.addVersion(3L, true);
        versionManager.addVersion(4L, false);
        versionManager.addVersion(5L, true);

        // Testing compatibility
        System.out.println(versionManager.isCompatible(1L, 2L));  // true
        System.out.println(versionManager.isCompatible(2L, 3L));  // true
        System.out.println(versionManager.isCompatible(3L, 4L));  // false
        System.out.println(versionManager.isCompatible(4L, 5L));  // true
        System.out.println(versionManager.isCompatible(1L, 5L));  // false
    }
}

class VersionManager {
    private Map<Long, Boolean> versionCompatibilityMap;

    public VersionManager() {
        versionCompatibilityMap = new HashMap<>();
    }

    public void addVersion(long versionNumber, boolean isCompatibleWithPrev) {
        versionCompatibilityMap.put(versionNumber, isCompatibleWithPrev);
    }

    public boolean isCompatible(long version1, long version2) {
        if (version1 == version2) {
            return true;
        }

        Boolean compatibility = versionCompatibilityMap.get(version2);
        if (compatibility != null && compatibility) {
            // Check if there are any incompatible versions between version1 and version2
            for (long i = version1 + 1; i < version2; i++) {
                Boolean isCompatibleWithPrev = versionCompatibilityMap.get(i);
                if (isCompatibleWithPrev != null && !isCompatibleWithPrev) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }
}



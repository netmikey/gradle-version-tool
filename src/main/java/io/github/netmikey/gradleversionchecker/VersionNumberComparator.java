package io.github.netmikey.gradleversionchecker;

import java.util.Comparator;

import org.springframework.stereotype.Component;

/**
 * @author mim
 */
@Component
public class VersionNumberComparator implements Comparator<String> {
    @Override
    public int compare(String version1, String version2) {
        String[] arr1 = version1.split("\\.");
        String[] arr2 = version2.split("\\.");

        int maxLength = Math.max(arr1.length, arr2.length);

        for (int i = 0; i < maxLength; i++) {
            if (i >= arr1.length || Integer.parseInt(arr1[i]) < Integer.parseInt(arr2[i])) {
                return -1;
            }
            if (i >= arr2.length || Integer.parseInt(arr1[i]) > Integer.parseInt(arr2[i])) {
                return 1;
            }
        }
        return 0;
    }

    /**
     * Do both of the specified versions have the same major version?
     * 
     * @param version1
     *            The lhs.
     * @param version2
     *            The rhs.
     * @return <code>true</code> iff both versions have the same major version
     *         part.
     */
    public boolean isSameMajor(String version1, String version2) {
        String[] arr1 = version1.split("\\.");
        String[] arr2 = version2.split("\\.");

        if (arr1.length == 0 || arr2.length == 0) {
            return false;
        } else {
            return arr1[0].equals(arr2[0]);
        }
    }
}
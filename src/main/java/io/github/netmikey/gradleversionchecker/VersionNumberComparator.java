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
}
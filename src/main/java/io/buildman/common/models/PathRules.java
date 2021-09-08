package io.buildman.common.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps a list of .gitignore-style rules, and handles eval paths against the rules.
 */
public class PathRules {

    private final List<String> rules = new ArrayList<>();

    public PathRules() {
    }


    public void addRules(String... lines) {
        for (String line : lines) {
            addRule(line);
        }
    }

    public void addRule(String line) {
        rules.add(line);
    }

    /**
     * @return true if we should ignore {@code path}
     */
    public boolean matches(String path, boolean isDirectory) {
        for (String rule : rules) {
            if (path.contains(rule)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return rules.toString();
    }

}

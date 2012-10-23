package org.jenkinsci.plugins.droolsplanner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to represent Task to Node assignment
 *
 * @author ogondza
 */
public final class NodeAssignements {

//    private final List<Integer> order;
    private final Map<Integer, String> assignments;

    public static NodeAssignements.Builder builder() {

        return new Builder();
    }

    public static final class Builder {

//        final List<Integer> order = new ArrayList<Integer>();
        final Map<Integer, String> assignments = new HashMap<Integer, String>();

        public NodeAssignements.Builder assign(final int id, final String nodeName) {

//            order.add(id);
            assignments.put(id, nodeName);
            return this;
        }

        public NodeAssignements build() {

            return new NodeAssignements(this);
        }
    }

    public NodeAssignements(final NodeAssignements.Builder builder) {

//        this.order = Collections.unmodifiableList(builder.order);
        this.assignments = Collections.unmodifiableMap(builder.assignments);
    }

    public String taskNodeName(final int taskId) {

        return assignments.get(taskId);
    }

    public int size() {

        return assignments.size();
    }
}
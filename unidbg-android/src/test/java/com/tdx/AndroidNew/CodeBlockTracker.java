package com.tdx.AndroidNew;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CodeBlockTracker {
    private List<Long> stack = new ArrayList<Long>();
    private Map<Long, CodeBranch> map = new TreeMap<>();
    public CodeBlockTracker() {

    }
    public int size() {
        return stack.size();
    }
    public void push(Long offset) {
        stack.add(offset);
    }
    public void pop() {
        int last = stack.size() - 1;
        if (last >= 0) {
            stack.remove(last);
        }
    }

}

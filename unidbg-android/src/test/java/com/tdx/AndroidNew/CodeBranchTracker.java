package com.tdx.AndroidNew;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CodeBranchTracker {
    private List<Long> stack = new ArrayList<Long>();
    private Map<Long, CodeBranch> map = new TreeMap<Long, CodeBranch>();
    public CodeBranchTracker() {

    }
    public int size() {
        return stack.size();
    }
    public CodeBranch get(Long offset) {
        return map.get(offset);
    }
    public void set(CodeBranch branch) {
        Long offset = branch.getInsOffset();
        CodeBranch v = map.get(offset);
        if (v != null) {
            throw new RuntimeException("CodeBranch is exist!");
        }
        map.put(offset, branch);
    }
    public int push(Long offset) {
        int i = stack.size();
        stack.add(offset);
        return i;
    }
    public void pop() {
        int last = stack.size() - 1;
        if (last >= 0) {
            stack.remove(last);
        }
    }

}

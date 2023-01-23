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
    public CodeBranch getByOffset(Long offset) {
        return map.get(offset);
    }
    public CodeBranch getByIndex(int i) {
        Long offset = stack.get(i);
        return map.get(offset);
    }
    public boolean isLast(CodeBranch branch) {
        Long blkOffset = branch.getBlkOffset();
        int last = stack.size() - 1;
        if (last >= 0) {
            Long lastOffset = stack.get(last);
            if (lastOffset.equals(blkOffset)) {
                return true;
            }
        }
        return false;
    }
    public void add(CodeBranch branch) {
        Long offset = branch.getBlkOffset();
        CodeBranch v = map.get(offset);
        if (v != null) {
            throw new RuntimeException("CodeBranch is exist!");
//            return;
        }
        map.put(offset, branch);
    }
    public void push(CodeBranch branch) {
        Long blkOffset = branch.getBlkOffset();
        stack.add(blkOffset);
    }
    public void pop() {
        int last = stack.size() - 1;
        if (last >= 0) {
            Long lastOffset = stack.get(last);
            System.out.println("pop branch: "+ Long.toHexString(lastOffset)+ ", last index:"+last);
            stack.remove(last);
        } else {
            System.out.println("pop not");
        }
    }
    public int findRing(long blkOffset) {
//        int last = stack.size() - 1;
//        for (int i = last; i >= 0; i--) {
//            CodeBranch b = stack.get(i);
//            if (b.getBlkOffset() == blkOffset) {
//                return i;
//            }
//        }
        return -1;
    }
}

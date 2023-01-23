package com.tdx.AndroidNew;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class CodeBlockMap {
    private Map<Long, CodeBlock> blockMap = new TreeMap<Long, CodeBlock>();
    private List<Long> blockList = new ArrayList<>();

    public Set<Long> keySets() {
        return blockMap.keySet();
    }
    public int size() {
        return blockMap.size();
    }
    public CodeBlock findBlock(Long offset) {
        CodeBlock block = blockMap.get(offset);
        return block;
    }
    public int offsetIndex(Long offset) {
        int len = blockList.size();
        if (len == 0) {
            return -1;
        }
        Long start = blockList.get(0);
        for (int i = 1; i < len; i++) {
            Long current = blockList.get(i);
            if (start <= offset && offset < current) {
                //find
                return i - 1;
            } else {
                start = current;
            }
        }
        return len - 1;
    }
    public CodeBlock searchBlock(Long offset) {
        int i = offsetIndex(offset);
        CodeBlock blk = blockMap.get(blockList.get(i));
        if (blk != null) {
            Long start = blk.getStart();
            Long end = blk.getEnd();
            if (offset >= start && offset <= end) {
                return blk;
            }
        }
        return null;
    }
    public void addBlock(CodeBlock block) {
        Long offset = block.getOffset();
        int idx = offsetIndex(offset);
        if (idx != -1 && blockList.get(idx) == offset) {
            throw new RuntimeException("block [" + offset + "] exist in list");
        }
        blockList.add(idx+1, offset);
//        block.setType(block.checkType());
        block.addRef();
        blockMap.put(offset, block);
    }

}

package com.tdx.AndroidNew;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CodeBlockList {
    private Map<String, ArrayList<Long>> mListMap = new HashMap<String, ArrayList<Long>>();
    public void resetList(String tag) {
        ArrayList<Long> list = mListMap.get(tag);
        if (list == null) {
            list = new ArrayList<>();
        }
        list.clear();
        mListMap.put(tag, list);
    }
    public void add(String tag, Long offset) {
        ArrayList<Long> list = mListMap.get(tag);
        if (list == null) {
            throw new RuntimeException("Codeblock List have not create!");
        }
        list.add(offset);
    }
    public Long get(String tag, int index) {
        ArrayList<Long> list = mListMap.get(tag);
        if (list == null) {
            throw new RuntimeException("Codeblock List have not create!");
        }
        return list.get(index);
    }
    public int size(String tag) {
        ArrayList<Long> list = mListMap.get(tag);
        if (list == null) {
            throw new RuntimeException("Codeblock List have not create! " + tag);
        }
        return list.size();
    }
}

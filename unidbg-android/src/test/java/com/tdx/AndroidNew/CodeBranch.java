package com.tdx.AndroidNew;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import capstone.api.Instruction;

public class CodeBranch {
    private long blkOffset;
    private long insOffset;
    private Instruction ins;
    private List<Integer> nzvcList = new ArrayList<Integer>();
    private List<Integer> indexList = new ArrayList<Integer>();
    private
    public CodeBranch(long insOffset, long blockOffset, Instruction ins) {
        this.blkOffset = blockOffset;
        this.insOffset = insOffset;
        this.ins = ins;
    }
    public long getBlkOffset() {return blkOffset;}
    public long getInsOffset() {return insOffset;}
//    public int getNzvc() {return nzvc;}
//    public void add(int index, int nzvc) {
//        nzvcList.add(nzvc);
//        indexList.add(index);
//    }

}

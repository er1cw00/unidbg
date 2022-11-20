package com.tdx.AndroidNew;

import java.util.Optional;

public class CodeBranch {
    private long blockOffset;
    private long insOffset;
    private int nzvc;
    private Optional<Boolean> branch;
    public CodeBranch(long blockOffset, long insOffset, int nzvc) {
        this.blockOffset = blockOffset;
        this.insOffset = insOffset;
        this.nzvc = nzvc;
        this.branch = Optional.empty();
    }
    public long getBlockOffset() {return blockOffset;}
    public long getInsOffset() {return insOffset;}
    public int getNzvc() {return nzvc;}

}

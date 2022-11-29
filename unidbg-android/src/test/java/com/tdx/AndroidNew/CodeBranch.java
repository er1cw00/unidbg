package com.tdx.AndroidNew;

import java.util.List;
import java.util.Optional;

public class CodeBranch {
    private long blkOffset;
    private long insOffset;
    private int nzvc;
    private List<int> index;
    private
    public CodeBranch(long blockOffset, long insOffset, int nzvc) {
        this.blkOffset = blockOffset;
        this.insOffset = insOffset;
        this.nzvc = nzvc;
    }
    public long getBlkOffset() {return blkOffset;}
    public long getInsOffset() {return insOffset;}
    public int getNzvc() {return nzvc;}


}

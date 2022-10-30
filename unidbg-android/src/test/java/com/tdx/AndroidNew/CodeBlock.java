package com.tdx.AndroidNew;

import java.util.Arrays;

import capstone.api.Instruction;

public class CodeBlock {
    private int ref;
    private long offset;
    private long base;
    private Instruction[] instruction;
    public CodeBlock(long offset, Instruction[] insns) {
        this.ref = 0;
        this.offset = offset;
        this.instruction = insns;
        this.base = 0x40000000;
    }
    public void addRef() {
        ref += 1;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("\t\"ref\": ").append(ref).append(", \n");
        sb.append("\t\"offset\": ").append(offset).append(", \n");
        int length = this.instruction.length;
        if (length > 0) {
            sb.append("\t\"ins\": [\n");
            for (int i = 0; i < length; i++) {
                Instruction ins = this.instruction[i];
                sb.append("\t\t\"0x" + Long.toHexString(ins.getAddress() - base) + "   " +ins.toString() + "\"");
                if (i + 1 < length) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("\t]\n");
        }
        sb.append("}");
        return sb.toString();
    }
}

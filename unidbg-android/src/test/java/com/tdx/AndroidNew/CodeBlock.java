package com.tdx.AndroidNew;

import net.dongliu.apk.parser.utils.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import capstone.Arm64_const;
import capstone.api.Instruction;
import capstone.api.RegsAccess;
import unicorn.Arm64Const;

public class CodeBlock {
    private int ref;
    private long offset;
    private long base;
    private CodeBlockType type;
    private List<Instruction> instruction;

    public CodeBlock(long base, long offset, Instruction[] insns) {
        this.ref = 0;
        this.base = base;
        this.offset = offset;
        this.instruction = Arrays.asList(insns);//new ArrayList<Instruction>(insns);
        this.type = CodeBlockType.UNKNOWN;
    }
    public void addRef() {
        ref += 1;
    }
    public void setType(CodeBlockType type) {
        this.type = type;
    }
    public CodeBlockType getType() {
        return this.type;
    }
    public Long getOffset() {return this.offset;}
    public int getCount() {return this.instruction.size();}
    public Long getStart() {
        Instruction ins = this.instruction.get(0);
        return ins.getAddress() - this.base;
    }
    public Long getEnd() {
        int len = this.instruction.size();
        Instruction ins = this.instruction.get(len - 1);
        return ins.getAddress() - this.base;
    }
    public CodeBlockType checkType() {
        int len = instruction.size();
        ArrayList<String> jmpCmds = new ArrayList(Arrays.asList("b.eq","b.ne","b.le","b.gt"));
        Instruction last = instruction.get(len - 1);
        if (len == 1 && last.getMnemonic().equals("b")) {
            return CodeBlockType.USED;
        } else if (last.getMnemonic().equals("bl") || last.getMnemonic().equals("ret")) {
            return CodeBlockType.USED;
        } else if (jmpCmds.contains(last.getMnemonic()) && (len == 2 || len == 4)) {
            Instruction last2 = instruction.get(len - 2);
            if (last2.getMnemonic().equals("cmp")) {
                capstone.api.arm64.Operand op = getArm64Operand(last2, 0);
                if (op.getType() == capstone.Arm64_const.ARM64_OP_REG) {
                    int regId = op.getValue().getReg();// int r = last2.mapToUnicornReg(regId);
                    String regName = last2.regName(regId);
                    if (regName.equals("w8")) {
                        return CodeBlockType.FAKE;
                    }
                }
            }
        }
//        for (int i = 0; i < len; i++) {
//            Instruction ins = instruction.get(i);
//            short[] regs = writeRegs(ins);
//            for (int j = 0; j < regs.length; j++) {
//            }
//            //System.out.printf("write:[" + w + "], read:["+ r + "], Instruction:" + ins + "\n");
//        }
        return CodeBlockType.USED;
    }

    public String toString(CodeBranch branch) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("\t\"type\": \"").append(type).append("\", \n");
        sb.append("\t\"offset\": \"0x").append(Long.toHexString(offset)).append("\", \n");

        if (branch != null) {
            sb.append("\t\"branch\": {\n").append(", \n");
            sb.append("\t\t\"offset\": \"").append(Long.toHexString(branch.getInsOffset())).append("\", \n");
            sb.append("\t\t\"cc\": \"").append(CodeBranch.ccLabel(branch.getCC())).append("\", \n");

            int length = branch.size();
            if (length > 0) {
                sb.append("\t\t\"list\": [\n");
                for (int i = 0; i < length; i++) {
                    Pair<Integer, Integer> pair = branch.get(i);
                    int idx = pair.getLeft().intValue();
                    int nzvc = pair.getRight().intValue();
                    sb.append("\t\t\t{\"index\": " + idx +", \"nzvc\": " + CodeBranch.nzvcLabel(pair.getRight()) + ", \"result\": " + CodeBranch.checkBranch(branch.getCC(), nzvc) +"}");
                    if (i + 1 < length) {
                        sb.append(",");
                    }
                    sb.append("\n");
                }
                sb.append("\t\t]\n");
            }
            sb.append("\t}");
        }
        int length = this.instruction.size();
        if (length > 0) {
            sb.append("\t\"ins\": [\n");
            for (int i = 0; i < length; i++) {
                Instruction ins = this.instruction.get(i);
                sb.append("\t\t\"0x" + Long.toHexString(ins.getAddress() - base) +
                        "   " + bytesToString(ins.getBytes()) +
                        "   " + ins.toString() + "\"");
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
    private String bytesToString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String v = Integer.toHexString(bytes[i] & 0XFF);
            if (v.length() == 1) {
                sb.append('0').append(v);
            } else {
                sb.append(v);
            }
        }
        return sb.toString();
    }
    private short[] readRegs(Instruction ins) {
        short[] regs;
        RegsAccess access = ins.regsAccess();
        if (access != null) {
            regs = access.getRegsRead();;
        } else {
            regs = new short[0];
        }

        return regs;
    }
    private short[] writeRegs(Instruction ins) {
        short[] regs;
        RegsAccess access = ins.regsAccess();
        if (access == null) {
            regs = access.getRegsWrite();;
        } else {
            regs = new short[0];
        }
        return regs;
    }
    private capstone.api.arm64.Operand[] getArm64Operands(Instruction ins) {
        capstone.api.arm64.OpInfo opInfo = (capstone.api.arm64.OpInfo) ins.getOperands();
        if (opInfo != null) {
            return opInfo.getOperands();
        }
        return null;
    }
    private capstone.api.arm64.Operand getArm64Operand(Instruction ins, int index) {
        capstone.api.arm64.Operand[] ops = getArm64Operands(ins);
        if (ops == null || ops.length == 0 || ops.length < index)  {
            return null;
        }
        return ops[index];
    }
}

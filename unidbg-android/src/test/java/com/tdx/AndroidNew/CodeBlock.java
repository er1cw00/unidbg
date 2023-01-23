package com.tdx.AndroidNew;

import net.dongliu.apk.parser.utils.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import capstone.Arm64_const;
import capstone.api.Instruction;
import capstone.api.RegsAccess;
import unicorn.Arm64Const;

public class CodeBlock {
    public static Integer CC_NONE = 0;
    public static Integer CC_TRUE = 1;
    public static Integer CC_FALSE = 2;
    private int ref;
    private long offset;
    private long base;
    private boolean start;
    private CodeBlockType type;
    private List<Instruction> instruction;
    private Map<Integer, Long> branch;
    private Map<Long, Instruction> patchMap;

    public CodeBlock(boolean start, long base, long offset, Instruction[] insns) {
        this.ref = 0;
        this.base = base;
        this.offset = offset;
        this.instruction = Arrays.asList(insns);//new ArrayList<Instruction>(insns);
        this.branch = new HashMap<>();
        this.start = start;
        this.type = CodeBlockType.UNKNOWN;
        this.patchMap = new HashMap<Long, Instruction>();
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
    public List<Long> getBlList() {
        List<Long> l = new ArrayList<>();
        int len = this.instruction.size();
        for (int i = 0; i < len; i++) {
            Instruction ins = this.instruction.get(i);
            String m = ins.getMnemonic();
            if (m.equals("bl") || m.equals("blr")) {
                l.add(ins.getAddress());
            }
        }
        return l;
    }
    public Long getStart() {
        Instruction ins = this.instruction.get(0);
        return ins.getAddress() - this.base;
    }
    public Long getEnd() {
        int len = this.instruction.size();
        Instruction ins = this.instruction.get(len - 1);
        return ins.getAddress() - this.base;
    }
    private boolean checkBlockPatched(Map<Long, Instruction> patchMap) {
        long start = getStart();
        long end = getEnd();
        boolean found = false;
        Set<Long> keys = patchMap.keySet();
        for (Long key : keys) {
            long offset = key.longValue();
            if (start <= offset && offset <= end) {
                found = true;
                this.patchMap.put(offset, patchMap.get(offset));
            }
        }
        return found;
    }
    public CodeBlockType checkType(Map<Long, Instruction> patchMap) {
        if (checkBlockPatched(patchMap)) {
            return CodeBlockType.USED;
        }
        if (offset == 0x436d8L) {
            System.out.println("asdf");
        }
        int len = instruction.size();
        ArrayList<String> jmpOps = new ArrayList(Arrays.asList("b.eq","b.ne","b.le","b.gt","b.lt","b.ge"));
        ArrayList<String> usedOps = new ArrayList(Arrays.asList("adr","adrp",
                                                                "ldr","ldrh","ldrb","ldur","ldurh","ldurb",
                                                                "str","strb","strh","stur","sturb","sturh",
                                                                "ldp","stp"));
        Instruction last = instruction.get(len - 1);
        if (len == 1 && last.getMnemonic().equals("b")) {
            return CodeBlockType.USED;
        } else if (last.getMnemonic().equals("bl") || last.getMnemonic().equals("ret")) {
            return CodeBlockType.USED;
        } else if (jmpOps.contains(last.getMnemonic()) && len >= 2) {
            boolean findCmpOp = false;
            for (int i = len - 2; i >= 0; i--) {
                Instruction ins = instruction.get(i);
                String mnemonic = ins.getMnemonic();
                if (mnemonic.equals("cmp")) {
                    capstone.api.arm64.Operand op = getArm64Operand(ins, 0);
                    if (op.getType() == capstone.Arm64_const.ARM64_OP_REG) {
                        int regId = op.getValue().getReg();
                        String regName = ins.regName(regId);
                        if (regName.equals("w8") ||
                                regName.equals("w9") ||
                                regName.equals("w10") ||
                                regName.equals("w12") ||
                                regName.equals("w15") ||
                                regName.equals("w26")) {
                            findCmpOp = true;
                        }
                    }
                } else if (usedOps.contains(mnemonic)) {
                    return CodeBlockType.USED;
                }
//                else if (!mnemonic.equals("mov") && !mnemonic.equals("movk") ) {
//
//                }
            }
            if (findCmpOp) {
                return CodeBlockType.FAKE;
            }
        }
        return CodeBlockType.USED;
    }
    public int branchSize() {
        return branch.size();
    }
    public Long getBranch(int type) {
        return branch.get(type);
    }
    public void putBranch(int type, Long val) {
        Long off = branch.get(type);
//        System.out.println("put branch <" + type + "," + Long.toHexString(val) + ">" +
//                         " for block " + Long.toHexString(offset));
        if (off != null) {
            if (!off.equals(val)) {
                throw new RuntimeException("try to replace branch <" + type + "," + Long.toHexString(off) + ">" +
                                            " for block " + Long.toHexString(offset) +
                                            " with " + Long.toHexString(val));
            }
            return;
        }
        branch.put(type, val);
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("\t\"type\": \"").append(type).append("\", \n");
        if (this.start) {
            sb.append("\t\"start\": true,\n");
        }
        sb.append("\t\"offset\": \"0x").append(Long.toHexString(offset)).append("\", \n");
        int s = branch.size();
        if (s > 0) {
            sb.append("\t\"branchs\": [\n");
            int i = 0;
            for (Integer key : branch.keySet()) {
                Long off = branch.get(key);
                sb.append("\t\t{")
                    .append("\"branch\": " + key + ", \"offset\": \"0x")
                    .append(Long.toHexString(off))
                    .append("\"")
                    .append("}");
                if (i + 1 < s) {
                    sb.append(",");
                }
                sb.append("\n");
                i += 1;
            }
            sb.append("\t],\n");
        }
        int length = this.instruction.size();
        if (length > 0) {
            sb.append("\t\"ins\": [\n");
            for (int i = 0; i < length; i++) {
                Instruction ins = this.instruction.get(i);
                long off = ins.getAddress() - base;
                if (patchMap.containsKey(off)) {
                    ins = patchMap.get(off);
                }
                sb.append("\t\t\"0x" + Long.toHexString(off) +
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

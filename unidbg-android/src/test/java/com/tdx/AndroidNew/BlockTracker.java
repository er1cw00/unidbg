package com.tdx.AndroidNew;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.backend.BlockHook;
import com.github.unidbg.arm.backend.UnHook;

import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import capstone.Capstone;
import capstone.api.Instruction;
import unicorn.Arm64Const;

public class BlockTracker {
    private Map<Long, CodeBlock> blockMap = new TreeMap<Long, CodeBlock>();
    private List<Long> blockList = new ArrayList<Long>();
    private AndroidEmulator emulator = null;
    private long base = 0;
    private long start = 0;
    private long end = 0;
    private String funcName;

    public enum BlockType {
        UNKNOWN("unknown", 0),
        USED("used", 1),
        FAKE("fake", 2);

        private String name;
        private int code;
        BlockType(String name, int code) {
            this.name = name;
            this.code = code;
        }
        public String toString() {
            return this.name;
        }
        public int getCode() {
            return this.code;
        }
    }
    private class CodeBlock {
        private int ref;
        private long offset;
        private long base;
        private BlockTracker.BlockType type;
        private Instruction[] instruction;
        public CodeBlock(long base, long offset, Instruction[] insns) {
            this.ref = 0;
            this.base = base;
            this.offset = offset;
            this.instruction = insns;
            this.type = BlockTracker.BlockType.UNKNOWN;
        }
        public void addRef() {
            ref += 1;
        }
        public void setType(BlockTracker.BlockType type) {
            this.type = type;
        }
        public BlockTracker.BlockType getType() {
            return this.type;
		}
        private capstone.api.arm64.Operand[] getArm64Operands(Instruction ins) {
            capstone.api.arm64.OpInfo opInfo = (capstone.api.arm64.OpInfo) ins.getOperands();
            if (opInfo != null) {
                return opInfo.getOperands();
            }
            return null;
        }
        public boolean checkFake() {
            int len = instruction.length;
            if (len >= 2) {
                Instruction last2 = instruction[len - 2];
                if (!last2.getMnemonic().equals("cmp")) {
                    return false;
                }
                capstone.api.arm64.Operand[] ops = getArm64Operands(last2);
                if (ops == null || ops.length < 2 || ops[0].getType() != capstone.Arm64_const.ARM64_OP_REG ) {
                    return false;
                }
                int regId = last2.mapToUnicornReg(ops[0].getValue().getReg());
                if (regId != Arm64Const.UC_ARM64_REG_W8) {
                    return false;
                }
                ArrayList<String> jmpCmds = new ArrayList(Arrays.asList("b.eq","b.ne","b.le","b.gt"));
                Instruction last = instruction[len - 1];
                if (jmpCmds.contains(last.getMnemonic())) {
                    return true;
                }
            }
        }
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("\t\"ref\": ").append(ref).append(", \n");
            sb.append("\t\"type\": \"").append(type).append("\", \n");
            sb.append("\t\"offset\": ").append(offset).append(", \n");
            int length = this.instruction.length;
            if (length > 0) {
                sb.append("\t\"ins\": [\n");
                for (int i = 0; i < length; i++) {
                    Instruction ins = this.instruction[i];
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
    }

    public BlockTracker(String funcName, AndroidEmulator emulator, long base) {
        this.funcName = funcName;
        this.emulator = emulator;
        this.base = base;
    }
    public void hook(long start, long end) {
        this.start = start;
        this.end = end;
        this.emulator.getBackend().hook_add_new(new BlockHook() {
            private UnHook unHook;
            @Override
            public void hookBlock(Backend backend, long address, int size, Object user) {
                long offset = address - base;
                Instruction[] insns = emulator.disassemble(address, size,0);
                track(offset, insns);
                //log(offset, size, insns);
            }
            @Override
            public void onAttach(UnHook unHook) {
                System.out.println("onAttach");
                if (this.unHook != null) {
                    throw new IllegalStateException();
                }
                this.unHook = unHook;
            }
            @Override
            public void detach() {
                if (unHook != null) {
                    unHook.unhook();
                    unHook = null;
                }
                System.out.println("detach block hook");
            }
        }, base + start, base + end,null);
    }

    public void save() {
        String rootDir = emulator.getFileSystem().getRootDir().toString();
        File file = new File(rootDir + File.separator + this.funcName + "_block.txt" );
        System.out.println("save block track: " + file.getAbsoluteFile());
        try {
            System.out.printf("saveCodeBlock list size: " + blockList.size() +",map size:" + blockMap.size() + "\n");
            file.createNewFile();
            FileWriter writer = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            bufferedWriter.write("[\n");
            for (int i = 0; i < blockList.size(); i++) {
                Long offset = blockList.get(i);
                CodeBlock blk = blockMap.get(offset);
                bufferedWriter.write(blk.toString());
                if (i + 1 < blockList.size()) {
                    bufferedWriter.write(",\n");
                }
            }
            bufferedWriter.write("]\n");
            bufferedWriter.close();
            writer.close();
        } catch (Exception e) {
            System.out.printf("exception:\n" + e);
        }
    }
    private void track(long offset, Instruction[] insns) {
        CodeBlock block = blockMap.get(offset);
        if (block == null) {
            block = new CodeBlock(this.base, offset, insns);
            block.addRef();
            blockMap.put(offset, block);
        } else {
            block.addRef();
        }
        blockList.add(offset);
    }
    private void log(long offset, long size, Instruction[] insns) {
        long pc = this.emulator.getBackend().reg_read(Arm64Const.UC_ARM64_REG_PC).longValue();
        System.out.println("block hook => pc:" + Long.toHexString(pc) +
                ",offset:" + String.format("0x%x", offset) +
                ",size:" + size);
    }
    public void scanBlock() {
        for (int i = 0; i < blockList.size(); i++) {
            Long offset = blockList.get(i);
            CodeBlock blk = blockMap.get(offset);
            blk.checkFake();
        }
    }
}

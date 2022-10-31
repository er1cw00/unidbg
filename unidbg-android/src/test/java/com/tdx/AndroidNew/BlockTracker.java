package com.tdx.AndroidNew;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.backend.BlockHook;
import com.github.unidbg.arm.backend.UnHook;

import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

    private class CodeBlock {
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
            block = new CodeBlock(offset, insns);
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
}

package com.tdx.AndroidNew;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.backend.BlockHook;
import com.github.unidbg.arm.backend.CodeHook;
import com.github.unidbg.arm.backend.UnHook;
import com.github.unidbg.spi.AbstractLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import capstone.Arm64_const;
import capstone.api.Instruction;
import unicorn.Arm64Const;

public class NagaHooker {

    private static final Log log = LogFactory.getLog(NagaHooker.class);
    private AndroidEmulator emulator = null;
    private long base = 0;
    private long start = 0;
    private long end = 0;
    private String funcName;
    private String mTag = "main";
    private Long currentBlock;
    private Long startBlock;
    private Long lastUsedBlock;
    private CodeBlockList blockList = new CodeBlockList();
    private CodeBlockMap blockMap = new CodeBlockMap();
    private CodeBranchTracker branchTracker = new CodeBranchTracker();
    private ArrayList<Long> runAddr = new ArrayList<Long>();
    private boolean printW8 = false;
    public NagaHooker(String funcName, AndroidEmulator emulator, long base) {
        this.funcName = funcName;
        this.emulator = emulator;
        this.base = base;
        this.startBlock = 0L;
        this.currentBlock = 0L;
        this.lastUsedBlock = 0L;
        resetTag("main");
    }
    public boolean isStop() {
        return branchTracker.size() == 0;
    }
    public void resetTag(String tag) {
        mTag = tag;
        this.blockList.resetList(tag);
        this.lastUsedBlock = 0L;
        log.warn("branchTracker.size:"+ branchTracker.size());
    }
    public void hook(long start, long end) {
        this.start = start;
        this.end = end;
        hookBlock(start, end);
        hookCode(start, end);
        //hookCode(0, 0x76f48);
    }
    public void saveBlock() {
        System.out.println("save blocks: " + blockMap.size());
    }
    public void saveTracker() {
        String rootDir = emulator.getFileSystem().getRootDir().toString();
        File file = new File(rootDir + File.separator + this.funcName +"_tk.txt" );
        System.out.println("save track: " + file.getAbsoluteFile());
        try {
            file.createNewFile();
            FileWriter writer = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            bufferedWriter.write("[\n");
            for (int i = 0; i < branchTracker.size(); i++) {
                CodeBranch branch = branchTracker.getByIndex(i);
                CodeBlock blk = blockMap.findBlock(branch.getBlkOffset());
                bufferedWriter.write(blk.toString());
                if (i + 1 < branchTracker.size()) {
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
    public void save(String tag) {
        String rootDir = emulator.getFileSystem().getRootDir().toString();
        File file = new File(rootDir + File.separator + this.funcName + "_"+ tag +"_block.txt" );
        System.out.println("save block track: " + file.getAbsoluteFile());
        try {
            System.out.printf("saveCodeBlock list size: " + blockList.size(tag) +",map size:" + blockMap.size() + "\n");
            file.createNewFile();
            FileWriter writer = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            bufferedWriter.write("[\n");
            for (int i = 0; i < blockList.size(tag); i++) {
                Long offset = blockList.get(tag, i);
                CodeBlock blk = blockMap.findBlock(offset);
                bufferedWriter.write(blk.toString());
                if (i + 1 < blockList.size(tag)) {
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
    private void hookBlock(long start, long end) {
        this.emulator.getBackend().hook_add_new(new BlockHook() {
            private UnHook unHook;
            @Override
            public void hookBlock(Backend backend, long address, int size, Object user) {
                trackBlock(backend, address, size);
                //logBlock(address, size);
            }
            @Override
            public void onAttach(UnHook unHook) {
                System.out.println("block hook onAttach");
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
    private void hookCode(long start, long end) {
        emulator.getBackend().hook_add_new(new CodeHook() {
            private UnHook unHook;
            @Override
            public void hook(Backend backend, long address, int size, Object user) {
                if (printW8) {
                    int w8 = emulator.getBackend().reg_read(Arm64Const.UC_ARM64_REG_W8).intValue();
                    //log.info("W8 reg:" + Integer.toHexString(w8));
                    printW8 = false;
                }
                trackCode(backend, address, size);
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
                System.out.println("detach code hook");
            }
        },base + start,base + end,null);
    }
    private void trackCode(Backend backend, long address, int size) {
        long offset = address - base;

        CodeBlock block = blockMap.findBlock(currentBlock);
        if (block == null) {
            log.warn("can not find current blk: " + Long.toHexString(currentBlock));
            return;
        }

        if (block.getType() != CodeBlockType.USED) {
            return;
        }
        Long blkOffset = block.getOffset();
        Instruction[] insns = emulator.disassemble(address, size,0);
        if (insns.length <= 0) {
            return;
        }
        Instruction ins = insns[0];
        if (!ins.getMnemonic().equals("csel")) {
            return;
        }
        capstone.api.arm64.OpInfo opInfo = (capstone.api.arm64.OpInfo) ins.getOperands();
        if (opInfo == null) {
            throw new RuntimeException("Track Code [" + ins.toString() + "] get OpInfo fail!");
        }
        capstone.api.arm64.Operand[] ops = opInfo.getOperands();
        if (ops == null || ops.length == 0)  {
            throw new RuntimeException("Track Code [" + ins.toString() + "] Operand is empty!");
        }

        int reg1 = ins.mapToUnicornReg(ops[0].getValue().getReg());
        int reg2 = ins.mapToUnicornReg(ops[1].getValue().getReg());
        int reg3 = ins.mapToUnicornReg(ops[2].getValue().getReg());
        int cc = opInfo.getCodeCondition();

        int w8 = emulator.getBackend().reg_read(reg1).intValue();
        int wx = emulator.getBackend().reg_read(reg2).intValue();
        int wy = emulator.getBackend().reg_read(reg3).intValue();
        int nzcv = emulator.getBackend().reg_read(Arm64Const.UC_ARM64_REG_NZCV).intValue();

        //emulator.getBackend().reg_write(Arm64Const.UC_ARM64_REG_W10, w9);
//        System.out.println("      w8:" + Integer.toHexString(w8) +
//                                ",wx:" + Integer.toHexString(wx) +
//                                ",wy:" + Integer.toHexString(wy) +
//                                ",nzvc:" + Integer.toHexString(nzcv));

//
        boolean result = CodeBranch.checkBranch(cc, nzcv);
//        log.info("trackCode:" + Long.toHexString(offset) +
//                ",blk:" + Long.toHexString(block.getOffset()) +
//                ",result:" + (result ? 1 : 2) +
//                "," + ins.getMnemonic() +
//                " " + ins.getOpStr() +
//                ",W8:" + Integer.toHexString(w8) +
//                ",Wx:" + Integer.toHexString(wx) +
//                ",Wy:" + Integer.toHexString(wy) +
//                ",nzvc:" + Integer.toHexString(nzcv) + ","+ CodeBranch.nzcvLabel(nzcv));

        CodeBranch branch = branchTracker.getByOffset(blkOffset);
        if (branch == null) {
            branch = new CodeBranch(offset, blkOffset, cc, ins);
            branch.set(0, result ? CodeBlock.CC_TRUE : CodeBlock.CC_FALSE);
            log.info("new branch :" + branch);
            branchTracker.add(branch);
            branchTracker.push(branch);
        } else {
            if (branchTracker.isLast(branch)) {
                if (branch.size() == 1) {
                    int b = branch.get(0);
                    if (b == CodeBlock.CC_TRUE) {
                        branch.set(1, CodeBlock.CC_FALSE);
                    } else if (b == CodeBlock.CC_FALSE) {
                        branch.set(1, CodeBlock.CC_TRUE);
                    }
                    emulator.getBackend().reg_write(reg2, wy);
                    emulator.getBackend().reg_write(reg3, wx);
                    branchTracker.pop();
                    log.info("find last branch need change:" + branch);
                }
            } else {
                int s = branch.size();
                if (s == 2) { // find ring, break;
                    log.warn("find Ring BlkOffset:" + Long.toHexString(blkOffset) + "; " +
                            "branch: " + branch);

                } else if (s == 1) {
                    // 按照之前的分支运行
                    log.info("find branch run as previous:" + branch);
                }
            }
        }
        printW8 = true;
        return;
    }
    private void trackBlock(Backend backend, long address, int size) {
        long offset = address - base;
        currentBlock = offset;
        if (startBlock == 0) {
            startBlock = offset;
        }
        CodeBlock block = blockMap.findBlock(offset);
        if (block == null) {
            Instruction[] insns = emulator.disassemble(address, size,0);
            block = new CodeBlock(this.base, offset, insns);
            blockMap.addBlock(block);
        } else {
            block.addRef();
        }
        blockList.add(mTag, offset);
        if (block.getType() == CodeBlockType.USED) {
            logBlock(address, size);
            if (lastUsedBlock != 0) {
                CodeBlock lastBlock = blockMap.findBlock(lastUsedBlock);
                CodeBranch lastBranch = branchTracker.getByOffset(lastUsedBlock);
                if (lastBlock != null) {
                    int type = CodeBlock.CC_NONE;
                    if (lastBranch != null) {
                        type = lastBranch.getLast();
                    }
                    try {
                        lastBlock.putBranch(type, offset);
                    } catch (Exception e) {
                        log.error("exception when put branch: " + e);
                    }
                }
            }
            lastUsedBlock = block.getOffset();
        }
    }
    private void logBlock(long address, int size) {
        long offset = address - base;
        long pc = this.emulator.getBackend().reg_read(Arm64Const.UC_ARM64_REG_PC).longValue();
        System.out.println("block hook => pc:" + Long.toHexString(pc) +
                            ",offset:" + String.format("0x%x", offset) +
                            ",size:" + size);

//      for(Instruction ins :insns) {
//           System.out.println("code hook => offset:" + String.format("0x%x", offset) +
//                                            ",code: " + ins.toString() + " [" +
//                                            ",Mnemonic:" + ins.getMnemonic() +
//                                            ",ops:" + ins.getOpStr() +
//                                            ",addr:" + Long.toHexString(ins.getAddress()) +
//                                            "]");
    }
    public void saveRunAddress() {
        String rootDir = emulator.getFileSystem().getRootDir().toString();
        File file = new File(rootDir+"/offset.txt");
        try {
            file.createNewFile();
            FileWriter writer =new FileWriter(file.getAbsoluteFile());
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            for(Long offset : runAddr) {
                bufferedWriter.write("0x"+Long.toHexString(offset)+"\r\n");
            }
            bufferedWriter.close();
            writer.close();
        } catch (Exception e) {
            System.out.printf("exception:\n" + e);
        }
        System.out.println("write run offset to " + file.getAbsoluteFile());
    }
    public void generateCallStack(List<String> tagList) {
//        for (String tag : tagList) {
//            CodeBlock last = null;
//            int length = blockList.size(tag);
//            for (int i = 0; i < length; i++) {
//                Long off = blockList.get(tag, i);
//                CodeBlock blk = blockMap.findBlock(off);
//                if (blk == null || blk.getType() != CodeBlockType.USED) {
//                    continue;
//                }
//                if (last != null) {
//                    last.addBranch(off);
//                }
//                last = blk;
//            }
//        }
        String rootDir = emulator.getFileSystem().getRootDir().toString();
        File file = new File(rootDir+"/blocks.txt");
        try {
            file.createNewFile();
            FileWriter writer =new FileWriter(file.getAbsoluteFile());
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            bufferedWriter.write("[\n");
            Long[] keys = blockMap.keySets().toArray(new Long[blockMap.size()]);
            int s = keys.length;
            for (int i = 0; i < s; i++) {
                Long off = keys[i];
                CodeBlock block = blockMap.findBlock(off);
                if (block.getType() == CodeBlockType.FAKE) {
                    continue;
                }
                bufferedWriter.write(block.toString());
                if (i + 1 < s) {
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
/*

            for (int i = 0; i < branchTracker.size(); i++) {
                CodeBranch branch = branchTracker.getByIndex(i);
                CodeBlock blk = blockMap.findBlock(branch.getBlkOffset());
                bufferedWriter.write(blk.toString());
                if (i + 1 < branchTracker.size()) {
                    bufferedWriter.write(",\n");
                }
            }


    public boolean scanBlock(int prefix) {
        if (prefix > blockList.size()) {
            return false;
        }
        for (int i = 0; i < prefix; i++) {
            Long offset = blockList.get(i);
            CodeBlock blk = mBlockMap.findBlock(offset);
            if (blk == null) {
                return false;
            }
            blk.setType(CodeBlockType.USED);
        }
        for (int i = prefix; i < blockList.size(); i++) {
            Long offset = blockList.get(i);
            CodeBlock blk = mBlockMap.findBlock(offset);
            if (blk == null) {
                return false;
            }
            blk.setType(blk.checkType());
        }
        return true;
    }
    */
}

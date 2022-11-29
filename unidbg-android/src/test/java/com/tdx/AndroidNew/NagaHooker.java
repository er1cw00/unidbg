package com.tdx.AndroidNew;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.backend.BlockHook;
import com.github.unidbg.arm.backend.CodeHook;
import com.github.unidbg.arm.backend.UnHook;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import capstone.Arm64_const;
import capstone.api.Instruction;
import unicorn.Arm64Const;

public class NagaHooker {


    private AndroidEmulator emulator = null;
    private long base = 0;
    private long start = 0;
    private long end = 0;
    private String funcName;
    private String mTag = "main";
    private CodeBlockList mBlockList = new CodeBlockList();
    private CodeBlockMap mBlockMap = new CodeBlockMap();
    private static int N_BIT = 31;
    private static int Z_BIT = 30;
    private static int C_BIT = 29;
    private static int V_BIT = 28;
    private static int N_MASK = (1 << N_BIT);
    private static int Z_MASK = (1 << Z_BIT);
    private static int C_MASK = (1 << C_BIT);
    private static int V_MASK = (1 << V_BIT);

    private static int getBit(int nzcv, int pos) {
        return (nzcv >> pos) & 0x00000001;
    }
    public NagaHooker(String funcName, AndroidEmulator emulator, long base) {
        this.funcName = funcName;
        this.emulator = emulator;
        this.base = base;
    }
    public void resetTag(String tag) {
        mTag = tag;
        this.mBlockList.resetList(tag);
    }
    public void hook(long start, long end) {
        this.start = start;
        this.end = end;
        hookBlock(start, end);
        hookCode(start,end);
    }

    public void save(String tag) {
        String rootDir = emulator.getFileSystem().getRootDir().toString();
        File file = new File(rootDir + File.separator + this.funcName + "_"+ tag +"_block.txt" );
        System.out.println("save block track: " + file.getAbsoluteFile());
        try {
            System.out.printf("saveCodeBlock list size: " + mBlockList.size(tag) +",map size:" + mBlockMap.size() + "\n");
            file.createNewFile();
            FileWriter writer = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            bufferedWriter.write("[\n");
            for (int i = 0; i < mBlockList.size(tag); i++) {
                Long offset = mBlockList.get(tag, i);
                CodeBlock blk = mBlockMap.findBlock(offset);
                bufferedWriter.write(blk.toString());
                if (i + 1 < mBlockList.size(tag)) {
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
                logBlock(address, size);
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
                //打印当前地址。这里要把unidbg使用的基址给去掉。
//                if (!mTag.equals("branch")) {
//                    return;
//                }

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
        CodeBlock block = mBlockMap.searchBlock(offset);
        if (block == null) {
            return;
        }
        if (block.getType() != CodeBlockType.USED ) {
            return;
        }
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
            return;
        }

        capstone.api.arm64.Operand[] ops = opInfo.getOperands();
        if (ops == null || ops.length == 0)  {
            return;
        }

        System.out.println("trackCode:" + Long.toHexString(offset) +
                           ",blk:" + Long.toHexString(block.getOffset()) +
                           "," + ins.getMnemonic() +
                           " " + ins.getOpStr());
        int reg1 = ins.mapToUnicornReg(ops[0].getValue().getReg());
        int reg2 = ins.mapToUnicornReg(ops[1].getValue().getReg());
        int reg3 = ins.mapToUnicornReg(ops[2].getValue().getReg());
        int cc = opInfo.getCodeCondition();

        int w8 = emulator.getBackend().reg_read(reg1).intValue();
        int wx = emulator.getBackend().reg_read(reg2).intValue();
        int wy = emulator.getBackend().reg_read(reg3).intValue();
        int nzcv = emulator.getBackend().reg_read(Arm64Const.UC_ARM64_REG_NZCV).intValue();
        //emulator.getBackend().reg_write(Arm64Const.UC_ARM64_REG_W10, w9);
        System.out.println("      w8:" + Integer.toHexString(w8) +
                                ",wx:" + Integer.toHexString(wx) +
                                ",wy:" + Integer.toHexString(wy) +
                                ",nzvc:" + Integer.toHexString(nzcv));

        return;
    }
    private void trackBlock(Backend backend, long address, int size) {
        long offset = address - base;
        CodeBlock block = mBlockMap.findBlock(offset);
        if (block == null) {
            Instruction[] insns = emulator.disassemble(address, size,0);
            block = new CodeBlock(this.base, offset, insns);
            mBlockMap.addBlock(block);
        } else {
            block.addRef();
        }
        mBlockList.add(mTag, offset);
    }
    private boolean checkBranch(int cc, int nzcv) {
        switch (cc) {
            case Arm64_const.ARM64_CC_INVALID:
                return false;
            case Arm64_const.ARM64_CC_EQ:
                return getBit(nzcv, Z_BIT) != 0;
            case Arm64_const.ARM64_CC_NE:
                return getBit(nzcv, Z_BIT) == 0;
            case Arm64_const.ARM64_CC_HS:
                return getBit(nzcv, C_BIT) != 0;
            case Arm64_const.ARM64_CC_LO:
                return getBit(nzcv, C_BIT) == 0;
            case Arm64_const.ARM64_CC_MI:
                return getBit(nzcv, N_BIT) != 0;
            case Arm64_const.ARM64_CC_PL:
                return getBit(nzcv, N_BIT) == 0;
            case Arm64_const.ARM64_CC_VS:
                return getBit(nzcv, V_BIT) != 0;
            case Arm64_const.ARM64_CC_VC:
                return getBit(nzcv, V_BIT) == 0;
            case Arm64_const.ARM64_CC_HI:
                return getBit(nzcv, C_BIT) != 0 && getBit(nzcv, Z_BIT) == 0;
            case Arm64_const.ARM64_CC_LS:
                return getBit(nzcv, C_BIT) == 0 && getBit(nzcv, Z_BIT) != 0;
            case Arm64_const.ARM64_CC_GE:
                return getBit(nzcv, N_BIT) == getBit(nzcv, V_BIT);
            case Arm64_const.ARM64_CC_LT:
                return getBit(nzcv, N_BIT) != getBit(nzcv, V_BIT);
            case Arm64_const.ARM64_CC_GT:
                return getBit(nzcv, Z_BIT) == 0 && getBit(nzcv, N_BIT) == getBit(nzcv, V_BIT);
            case Arm64_const.ARM64_CC_LE:
                return getBit(nzcv, Z_BIT) == 1 && getBit(nzcv, N_BIT) != getBit(nzcv, V_BIT);
            case Arm64_const.ARM64_CC_AL:
                return true;
            case Arm64_const.ARM64_CC_NV:
                return false;

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
/*
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

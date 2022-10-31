package com.tdx.AndroidNew;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.backend.CodeHook;
import com.github.unidbg.arm.backend.UnHook;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;
import java.util.TreeMap;

import capstone.api.Instruction;

public class CodeTracker {

    private AndroidEmulator emulator = null;
    private long base = 0;
    private long start = 0;
    private long end = 0;
    private String funcName;
    private Map<Long, Integer> codeMap = new TreeMap<Long, Integer>();

    public CodeTracker(String funcName, AndroidEmulator emulator, long base) {
        this.funcName = funcName;
        this.emulator = emulator;
        this.base = base;
    }
    public void hook(long start, long end) {
        emulator.getBackend().hook_add_new(new CodeHook() {
            private UnHook unHook;
            @Override
            public void hook(Backend backend, long address, int size, Object user) {
                //打印当前地址。这里要把unidbg使用的基址给去掉。
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
                System.out.println("detach code hook");
            }
        },base + start,base + end,null);
    }
    public void save() {
        String rootDir = emulator.getFileSystem().getRootDir().toString();
        File file = new File(rootDir + File.pathSeparator + this.funcName + "_code.txt" );
        try {
            file.createNewFile();
            FileWriter writer =new FileWriter(file.getAbsoluteFile());
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            for(Long offset : codeMap.keySet()) {
                bufferedWriter.write("0x"+Long.toHexString(offset)+"\r\n");
            }
            bufferedWriter.close();
            writer.close();
        } catch (Exception e) {
            System.out.printf("exception:\n" + e);
        }
        System.out.println("write run offset to " + file.getAbsoluteFile());
    }
    private void track(long offset, Instruction[] insns) {
        Integer value = codeMap.get(offset);
        if (value == null) {
            codeMap.put(offset, 1);
        } else {
            codeMap.put(offset, value + 1);
        }
    }
    private void log(long offset, long size, Instruction[] insns) {
        System.out.println("code hook => offset:" + String.format("0x%x", offset));
        for(Instruction ins :insns) {
//            System.out.println("code hook => offset:" + String.format("0x%x", offset) +
//                                            ",code: " + ins.toString() + " [" +
//                                            ",Mnemonic:" + ins.getMnemonic() +
//                                            ",ops:" + ins.getOpStr() +
//                                            ",addr:" + Long.toHexString(ins.getAddress()) +
//                                            "]");
        }
    }
}

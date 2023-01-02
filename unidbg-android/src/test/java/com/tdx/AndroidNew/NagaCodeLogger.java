package com.tdx.AndroidNew;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.backend.CodeHook;
import com.github.unidbg.arm.backend.UnHook;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import unicorn.Arm64Const;

public class NagaCodeLogger {
    private static final Log log = LogFactory.getLog(NagaHooker.class);
    private AndroidEmulator emulator = null;
    private long base = 0;
    private long start = 0;
    private long end = 0;
    private String funcName;
    private boolean isLog;
    public NagaCodeLogger(AndroidEmulator emulator, String funcName,  long base) {
        this.funcName = funcName;
        this.emulator = emulator;
        this.base = base;
        this.isLog = false;
    }
    public void start() {
        this.isLog = true;
    }
    public void stop() {
        this.isLog = false;
    }
    public void hook(long start, long end) {
        emulator.getBackend().hook_add_new(new CodeHook() {
            private UnHook unHook;
            @Override
            public void hook(Backend backend, long address, int size, Object user) {
                if (isLog) {
                    long offset = address - base;
                    log.info(Long.toHexString(offset));
                    if (offset == 0x6da2cL) {
                        long x8 = backend.reg_read(Arm64Const.UC_ARM64_REG_X8).longValue();
                        System.out.println("X8: " + Long.toHexString(x8));
                    }
                }
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
}

package com.tdx.AndroidNew;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.backend.CodeHook;

import com.github.unidbg.arm.backend.DynarmicFactory;
import com.github.unidbg.debugger.Debugger;
import com.github.unidbg.debugger.DebuggerType;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import unicorn.Unicorn;
import com.github.unidbg.arm.backend.UnHook;

public class NagaLoader {

    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;
    private Debugger debugger;
//    private final DvmClass xloaderDVM;
    private final boolean logging;
    private final String packageName = "com.tdx.AndroidNew";
    private final String rootDir = "/Users/wadahana/Desktop/tdx/rootfs";
    private final String libPath = "/Users/wadahana/Desktop/tdx/libxloader.so";
//    private final String libPath = "/Users/wadahana/Desktop/tdx/xloader/xloader.so";
    public static void main(String[] args) {
        NagaLoader xloader = new NagaLoader(true);
        //xloader.load();
        //xloader.destroy();
    }
    NagaLoader(boolean logging) {
        this.logging = logging;
//        Logger.getLogger("com.github.unidbg.linux.ModuleSymbol").setLevel(Level.DEBUG);
//        Logger.getLogger("com.github.unidbg.linux.AndroidElfLoader").setLevel(Level.DEBUG);
        Logger.getLogger("com.github.unidbg.linux.libxloader.so").setLevel(Level.DEBUG);
        Logger.getLogger("com.github.unidbg.linux.LinuxModule").setLevel(Level.DEBUG);
        emulator = AndroidEmulatorBuilder.for64Bit()
                .setProcessName(packageName)
                .addBackendFactory(new DynarmicFactory(true))
                .setRootDir(new File(rootDir))
                .build(); // 创建模拟器实例，要模拟32位或者64位，在这里区分

        final Memory memory = emulator.getMemory(); // 模拟器的内存操作接口
        long baseAddr = memory.MMAP_BASE;
        memory.setLibraryResolver(new AndroidResolver(23)); // 设置系统类库解析
        //initRootfs();

        emulator.getBackend().hook_add_new(new CodeHook() {
            private UnHook unHook;
            @Override
            public void hook(Backend backend, long address, int size, Object user) {
                //打印当前地址。这里要把unidbg使用的基址给去掉。
                //System.out.println(String.format("0x%x",address-baseAddr));
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
                System.out.println("detach");
            }
        },baseAddr,baseAddr+0x2063FF,null);
//        },0x4003CFB8,0x4003D450,null);

//        debugger = emulator.attach(DebuggerType.ANDROID_SERVER_V7);
//        debugger = emulator.attach(DebuggerType.CONSOLE);
//        debugger.addBreakPoint(baseAddr + 0x7E90);
//        debugger.addBreakPoint(baseAddr + 0x30DFC);

        UnidbgPointer p = memory.pointer(baseAddr + 0x90890);

        vm = emulator.createDalvikVM(); // 创建Android虚拟机
        vm.setVerbose(logging); // 设置是否打印Jni调用细节
        DalvikModule dm = vm.loadLibrary(new File(libPath), false); // 加载libttEncrypt.so到unicorn虚拟内存，加载成功以后会默认调用init_array等函数
        module = dm.getModule(); // 加载好的libttEncrypt.so对应为一个模块
        System.out.println("module base:" + Long.toHexString(module.base));
        //dm.callJNI_OnLoad(emulator); // 手动执行JNI_OnLoad函数
    }
    private void destroy() {
        try {
            int pid = emulator.getPid();
            String rootDir = emulator.getFileSystem().getRootDir().toString();
            String procDir = rootDir + "/proc/" + pid;
            FileUtils.deleteDirectory(new File(procDir));
            emulator.close();
        } catch (Exception e) {
            System.out.printf("exception:\n" + e);
        }
        System.out.println("emulator destroy...");
    }
    private void createSymLink(String dest, String symName) throws IOException {
        Path destFilePath = Paths.get(dest);
        Path symLinkPath = Paths.get(symName);
        Files.createSymbolicLink(symLinkPath, destFilePath);
    }
    private void initRootfs() {
        int pid = emulator.getPid();
        String rootDir = emulator.getFileSystem().getRootDir().toString();
        System.out.println("rootDir:" + rootDir);
        String procDir = rootDir + "/proc/" + pid;
        String selfDir = rootDir + "/proc/self";
        System.out.println("procDir:" + procDir);
        System.out.println("selfDir:" + selfDir);
        try {
            FileUtils.forceMkdir(new File(procDir));
            createSymLink( selfDir+"/cmdline", procDir + "/cmdline");
            createSymLink( selfDir+"/status", procDir + "/status");
        } catch (Exception e) {
            System.out.printf("exception:\n" + e);
        }

    }
}
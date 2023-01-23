package com.tdx.AndroidNew;

import com.github.unidbg.AbstractEmulator;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;

import com.github.unidbg.arm.backend.DynarmicFactory;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.debugger.Debugger;
import com.github.unidbg.debugger.DebuggerType;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.spi.InitFunction;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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

    class Function {
        public String name;
        public long start;
        public long end;
        public Function(String name, long start, long end) {
            this.name = name;
            this.start = start;
            this.end = end;
        }
    };
    private List<Function> funcList = new ArrayList<>();
    public static void main(String[] args) {
        initLogger();
        NagaLoader loader = new NagaLoader(true);
//        loader.load();
//        loader.destroy();
    }

    public void initFuncList() {
        funcList.add(new Function("sub_4aef4", 0x4aef4L, 0x4b20cL));
        funcList.add(new Function("sub_4a8b4", 0x4a8b4L, 0x4aef0L));
        funcList.add(new Function("sub_4c398", 0x4c398L, 0x4cdf0L));
        funcList.add(new Function("sub_4319c", 0x4319cL, 0x43a9cL));
        funcList.add(new Function("sub_52764", 0x52764L, 0x5297CL));
        funcList.add(new Function("sub_5419c", 0x5419cL, 0x542DCL));
        funcList.add(new Function("sub_5C3fC", 0x5C3fCL, 0x5C6A4L));
        funcList.add(new Function("sub_5C6A8", 0x5C6A8L, 0x5C910L));
        funcList.add(new Function("sub_37588", 0x37588L, 0x37784L));
        funcList.add(new Function("sub_37788", 0x37788L, 0x379c4L));
        funcList.add(new Function("init_proc", 0x3CFB8L, 0x3D450L));
    }
    NagaLoader(boolean logging) {
        this.logging = logging;
        emulator = AndroidEmulatorBuilder.for64Bit()
                .setProcessName(packageName)
                .addBackendFactory(new Unicorn2Factory(true))
                //.addBackendFactory(new DynarmicFactory(true))
                .setRootDir(new File(rootDir))
                .build(); // 创建模拟器实例，要模拟32位或者64位，在这里区分

        final Memory memory = emulator.getMemory(); // 模拟器的内存操作接口
        final long base = memory.MMAP_BASE;
        memory.setLibraryResolver(new AndroidResolver(23)); // 设置系统类库解析
        // libxloader.si total size : 0x2063FF
        //initRootfs();
        initDebuger();
        initFuncList();


        vm = emulator.createDalvikVM(); // 创建Android虚拟机
        vm.setVerbose(logging); // 设置是否打印Jni调用细节
        DalvikModule dm = vm.loadLibrary(new File(libPath), false); // 加载libttEncrypt.so到unicorn虚拟内存，加载成功以后会默认调用init_array等函数
        module = dm.getModule(); // 加载好的libttEncrypt.so对应为一个模块
        List<InitFunction> funcs = module.getInitFunctions();
        List<String> tagList = new ArrayList<>();
        System.out.println("module base:" + Long.toHexString(module.base) + ",init func:" + funcs.size());

        Function func = funcList.get(0);
        String funName = func.name;
        long funStart = func.start;
        long funEnd = func.end;

        NagaHooker blkHooker = new NagaHooker(funName, base, emulator, debugger);
        //blkHooker.hook(0x3CFB8, 0x3D450);
        blkHooker.hook(funStart, funEnd);

        NagaCodeLogger codeLogger = new NagaCodeLogger(emulator, funName, base);
        codeLogger.hook(0, 0x76f48);
//        InitFunction func = funcs.get(0);
        blkHooker.resetTag("main");
        tagList.add("main");
//        codeLogger.start();
        module.callFunction(emulator, funStart);
        //func.call(emulator);
        //codeLogger.stop();
        blkHooker.save("main");
        int i = 0;
        for(i = 0; i < 10 && !blkHooker.isStop(); i++) {
            String tag = "Tag" + i;
//            if (i == 1) {codeLogger.start();}
            tagList.add(tag);
            System.out.println("call func with tag: " + tag + " >>>>>>");
            blkHooker.resetTag(tag);
            module.callFunction(emulator, funStart);
            codeLogger.stop();
            //func.call(emulator);
            blkHooker.save(tag);
        }
        blkHooker.saveCallStack();
        System.out.println(">>>>>>> i:"+ i);
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
    private void initDebuger() {
//        DebuggerType dbgType = DebuggerType.ANDROID_SERVER_V7;
//      DebuggerType dbgType = DebuggerType.GDB_SERVER;
        DebuggerType dbgType = DebuggerType.CONSOLE;
        long base = 0x40000000;
//        debugger = emulator.attach();
        debugger = emulator.attach(dbgType);
        //debugger.addBreakPoint(base + 0x3d19c);
//        debugger.addBreakPoint(base + 0x30DFC);
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
    private static void initLogger() {
        Properties properties = new Properties();

        properties.setProperty("log4j.debug", "false");
        properties.setProperty("log4j.reset", "true");
        properties.setProperty("log4j.rootLogger", "INFO, CONSOLE"); // add "FILE" to enable File Appender

        properties.setProperty("log4j.appender.CONSOLE", "org.apache.log4j.ConsoleAppender");
        properties.setProperty("log4j.appender.Threshold", "DEBUG");
        properties.setProperty("log4j.appender.CONSOLE.Target", "System.out");
        properties.setProperty("log4j.appender.CONSOLE.layout", "org.apache.log4j.PatternLayout");

        properties.setProperty("log4j.appender.FILE", "org.apache.log4j.FileAppender");
        properties.setProperty("log4j.appender.FILE.File", "NagaLoader.log");
        properties.setProperty("log4j.appender.FILE.Append", "false");
        properties.setProperty("log4j.appender.FILE.layout", "org.apache.log4j.PatternLayout");
        PropertyConfigurator.configure(properties);

//        Logger.getLogger("com.github.unidbg.linux.ModuleSymbol").setLevel(Level.DEBUG);
//        Logger.getLogger("com.github.unidbg.linux.AndroidElfLoader").setLevel(Level.DEBUG);
//        Logger.getLogger("com.github.unidbg.linux.libxloader.so").setLevel(Level.DEBUG);
//        Logger.getLogger("com.github.unidbg.linux.LinuxModule").setLevel(Level.DEBUG);
        Logger.getLogger("com.github.unidbg.linux.ARM64SyscallHandler").setLevel(Level.DEBUG);
    }
}

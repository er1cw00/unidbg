package com.tdx.AndroidNew;

import net.dongliu.apk.parser.utils.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import capstone.Arm64_const;
import capstone.api.Instruction;

public class CodeBranch {

    public static int N_BIT = 31;
    public static int Z_BIT = 30;
    public static int C_BIT = 29;
    public static int V_BIT = 28;
    public static int N_MASK = (1 << N_BIT);
    public static int Z_MASK = (1 << Z_BIT);
    public static int C_MASK = (1 << C_BIT);
    public static int V_MASK = (1 << V_BIT);

    private static int getBit(int nzcv, int pos) {
        return (nzcv >> pos) & 0x00000001;
    }
    private long blkOffset;
    private long insOffset;
    private int cc;
    private Instruction ins;

    private List<Integer> nzvcList = new ArrayList<Integer>();
    private List<Integer> indexList = new ArrayList<Integer>();

    public CodeBranch(long insOffset, long blockOffset, int cc, Instruction ins) {
        this.blkOffset = blockOffset;
        this.insOffset = insOffset;
        this.cc = cc;
        this.ins = ins;
    }
    public long getBlkOffset() {return blkOffset;}
    public long getInsOffset() {return insOffset;}
    public int getCC() {return cc;}
    public int size() {return indexList.size();}
    public Pair<Integer, Integer> get(int i) {
        int idx = indexList.get(i);
        int nzvc = nzvcList.get(i);
        Pair<Integer, Integer> pair = new Pair<>(idx, nzvc);
        return pair;
    }
    public void add(int index, int nzvc) {
        nzvcList.add(nzvc);
        indexList.add(index);
    }
    static public String nzvcLabel(int nzvc) {
        String label = "";
        if (getBit(nzvc, N_BIT) != 0) {
            label = "N";
        } else {
            label = "-";
        }
        if (getBit(nzvc, Z_BIT) != 0) {
            label += "Z";
        } else {
            label += "-";
        }
        if (getBit(nzvc, C_BIT) != 0) {
            label += "C";
        } else {
            label += "-";
        }
        if (getBit(nzvc, V_BIT) != 0) {
            label += "V";
        } else {
            label += "-";
        }
        return label;
    }
    static public String ccLabel(int cc) {
        switch (cc) {
            case Arm64_const.ARM64_CC_INVALID:
                return "INVALID";
            case Arm64_const.ARM64_CC_EQ:
                return "EQ";
            case Arm64_const.ARM64_CC_NE:
                return "NE";
            case Arm64_const.ARM64_CC_HS:
                return "HS";
            case Arm64_const.ARM64_CC_LO:
                return "LO";
            case Arm64_const.ARM64_CC_MI:
                return "MI";
            case Arm64_const.ARM64_CC_PL:
                return "PL";
            case Arm64_const.ARM64_CC_VS:
                return "VS";
            case Arm64_const.ARM64_CC_VC:
                return "VC";
            case Arm64_const.ARM64_CC_HI:
                return "HI";
            case Arm64_const.ARM64_CC_LS:
                return "LS";
            case Arm64_const.ARM64_CC_GE:
                return "GE";
            case Arm64_const.ARM64_CC_LT:
                return "LT";
            case Arm64_const.ARM64_CC_GT:
                return "GT";
            case Arm64_const.ARM64_CC_LE:
                return "LE";
            case Arm64_const.ARM64_CC_AL:
                return "AL";
            case Arm64_const.ARM64_CC_NV:
                return "NV";
        }
        return "UNKNOW";
    }
    static public boolean checkBranch(int cc, int nzcv) {
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
        return false;
    }
}

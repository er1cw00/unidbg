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
    private int last;
    private Instruction ins;
    int[] list = new int[2];

    public CodeBranch(long insOffset, long blockOffset, int cc, Instruction ins) {
        this.blkOffset = blockOffset;
        this.insOffset = insOffset;
        this.cc = cc;
        this.ins = ins;
        this.last = 0;
        this.list[0] = 0;
        this.list[1] = 0;

    }
    public long getBlkOffset() {return blkOffset;}
    public long getInsOffset() {return insOffset;}
    public int getCC() {return cc;}
    public int size() {
        int s = 0;
        if (list[0] != 0) {
            if (list[1] != 0) {
                return 2;
            }
            return 1;
        }
        return 0;
    }
    public int get(int i) {
        return list[i];
    }
    public void set(int index, int value) {
        list[index] = value;
    }
    public void setLast(int cc) {
        last = cc;
    }
    public int getLast() {
        return last;
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("\t\"blkOffset\": \"0x").append(Long.toHexString(blkOffset)).append("\", \n");
        sb.append("\t\"insOffset\": \"0x").append(Long.toHexString(insOffset)).append("\", \n");
        sb.append("\t\"cc\": \"").append(CodeBranch.ccLabel(getCC())).append("\", \n");
        int length = size();
        if (length > 0) {
            sb.append("\t\"branch\": [");
            for (int i = 0; i < length; i++) {
                int b = list[i];
                sb.append(Integer.toString(b));
                if (i + 1 < length) {
                    sb.append(",");
                }
            }
            sb.append("]\n");
        }
        sb.append("}");
        return sb.toString();
    }
    static public String nzcvLabel(int nzvc) {
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


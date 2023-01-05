package com.tdx.AndroidNew;

import net.dongliu.apk.parser.utils.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import capstone.Arm64_const;
import capstone.api.Instruction;
/*
*
0000 = EQ - Z set (equal，相等)
0001 = NE - Z clear (not equal，不相等)
0010 = CS - C set (unsigned higher or same，无符号大于或等于)
0011 = CC - C clear (unsigned lower，无符号小于)
0100 = MI - N set (negative，负数)
0101 = PL - N clear (positive or zero，正数或零)
0110 = VS - V set (overflow，溢出)
0111 = VC - V clear (no overflow，未溢出)
1000 = HI - C set and Z clear (unsigned higher，无符号大于)
1001 = LS - C clear or Z set (unsigned lower or same，无符号小于或等于)
1010 = GE - N set and V set, or N clear and V clear (greater or equal，带符号大于或等于)
1011 = LT - N set and V clear, or N clear and V set (less than，带符号小于)
1100 = GT - Z clear, and either N set and V set, or N clear and V clear (greater than，带符号大于)
1101 = LE - Z set, or N set and V clear, or N clear and V set (less than or equal，带符号小于或等于)
1110 = AL - always
1111 = NV - never

*
* */
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
    private static int setBit(int nzcv, int pos) { return (1 << pos) | nzcv ;}
    private static int clearBit(int nzcv, int pos) { return (~(1 << pos)) & nzcv;}
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
    static public int getNZCV(int cc, int nzcv, boolean result) {
        switch (cc) {
            case Arm64_const.ARM64_CC_EQ:
                if (result) {nzcv = setBit(nzcv, Z_BIT);
                } else { nzcv = clearBit(nzcv, Z_BIT);}
                break;
            case Arm64_const.ARM64_CC_NE:
                if (result) { nzcv = clearBit(nzcv, Z_BIT);
                } else { nzcv = setBit(nzcv, Z_BIT); }
                break;
            case Arm64_const.ARM64_CC_HS:
                if (result) { nzcv = setBit(nzcv, C_BIT);
                } else { nzcv = clearBit(nzcv, C_BIT); }
                break;
            case Arm64_const.ARM64_CC_LO:
                if (result) { nzcv = clearBit(nzcv, C_BIT);
                } else { nzcv = setBit(nzcv, C_BIT); }
                break;
            case Arm64_const.ARM64_CC_MI:
                if (result) {  nzcv = setBit(nzcv, N_BIT);
                } else { nzcv = clearBit(nzcv, N_BIT);}
                break;
            case Arm64_const.ARM64_CC_PL:
                if (result) { nzcv = clearBit(nzcv, N_BIT);
                } else { nzcv = setBit(nzcv, N_BIT); }
                break;
            case Arm64_const.ARM64_CC_VS:
                if (result) { nzcv = setBit(nzcv, V_BIT);
                } else {nzcv = clearBit(nzcv, V_BIT);}
                break;
            case Arm64_const.ARM64_CC_VC:
                if (result) { nzcv = clearBit(nzcv, V_BIT);
                } else { nzcv = setBit(nzcv, V_BIT); }
                break;
            case Arm64_const.ARM64_CC_HI:
                if (result) {
                    nzcv = setBit(nzcv, C_BIT);
                    nzcv = clearBit(nzcv, Z_BIT);
                } else {
                    nzcv = clearBit(nzcv, C_BIT);
                    nzcv = setBit(nzcv, Z_BIT);
                }
                break;
            case Arm64_const.ARM64_CC_LS:
                if (result) {
                    nzcv = clearBit(nzcv, C_BIT);
                    nzcv = setBit(nzcv, Z_BIT);
                } else {
                    nzcv = setBit(nzcv, C_BIT);
                    nzcv = clearBit(nzcv, Z_BIT);
                }
                break;
            case Arm64_const.ARM64_CC_GE:
                if (result) {
                    nzcv = setBit(nzcv, N_BIT);
                    nzcv = setBit(nzcv, V_BIT);
                } else {
                    nzcv = setBit(nzcv, N_BIT);
                    nzcv = clearBit(nzcv, V_BIT);
                }
                break;
            case Arm64_const.ARM64_CC_LT:
                if (result) {
                    nzcv = setBit(nzcv, N_BIT);
                    nzcv = clearBit(nzcv, V_BIT);
                } else {
                    nzcv = setBit(nzcv, N_BIT);
                    nzcv = setBit(nzcv, V_BIT);
                }
                break;
            case Arm64_const.ARM64_CC_GT:
                if (result) {
                    nzcv = clearBit(nzcv, Z_BIT);
                    nzcv = setBit(nzcv, N_BIT);
                    nzcv = setBit(nzcv, V_BIT);
                } else {
                    nzcv = setBit(nzcv, Z_BIT);
                    nzcv = setBit(nzcv, N_BIT);
                    nzcv = clearBit(nzcv, V_BIT);
                }
                break;
            case Arm64_const.ARM64_CC_LE:
                if (result) {
                    nzcv = setBit(nzcv, Z_BIT);
                    nzcv = setBit(nzcv, N_BIT);
                    nzcv = clearBit(nzcv, V_BIT);
                } else {
                    nzcv = clearBit(nzcv, Z_BIT);
                    nzcv = setBit(nzcv, N_BIT);
                    nzcv = setBit(nzcv, V_BIT);
                }
                break;
            case Arm64_const.ARM64_CC_AL:
                if (result) {
                    nzcv = setBit(nzcv, N_BIT);
                    nzcv = setBit(nzcv, Z_BIT);
                    nzcv = setBit(nzcv, C_BIT);
                    nzcv = setBit(nzcv, V_BIT);
                } else {
                    nzcv = clearBit(nzcv, N_BIT);
                    nzcv = clearBit(nzcv, Z_BIT);
                    nzcv = clearBit(nzcv, C_BIT);
                    nzcv = clearBit(nzcv, V_BIT);
                }
                break;
            case Arm64_const.ARM64_CC_NV:
                if (!result) {
                    nzcv = setBit(nzcv, N_BIT);
                    nzcv = setBit(nzcv, Z_BIT);
                    nzcv = setBit(nzcv, C_BIT);
                    nzcv = setBit(nzcv, V_BIT);
                } else {
                    nzcv = clearBit(nzcv, N_BIT);
                    nzcv = clearBit(nzcv, Z_BIT);
                    nzcv = clearBit(nzcv, C_BIT);
                    nzcv = clearBit(nzcv, V_BIT);
                }
                break;
        }
        return  nzcv;
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


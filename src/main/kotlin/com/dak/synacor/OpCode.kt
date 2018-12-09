package com.dak.synacor

enum class OpCode(val opCode: Int, val numOfArguments: Int) {
    HALT(0, 0),
    SET(1, 2),
    PUSH(2, 1),
    POP(3, 1),
    EQ(4, 3),
    GT(5, 3),
    JMP(6, 1),
    JT(7, 2),
    JF(8, 2),
    ADD(9, 3),
    MULT(10, 3),
    MOD(11, 3),
    AND(12, 3),
    OR(13, 3),
    NOT(14, 2),
    RMEM(15, 2),
    WMEM(16, 2),
    CALL(17, 1),
    RET(18, 0),
    OUT(19, 1),
    IN(20, 1),
    NOOP(21, 0);

    companion object {
        fun isOpCode(opCodeInt: Int): Boolean = opCodeInt in OpCode.values().map(OpCode::opCode)
        fun fromInt(opCodeInt: Int): OpCode = OpCode.values().getOrNull(opCodeInt)?: throw RuntimeException("invalid op code $opCodeInt")
    }
}
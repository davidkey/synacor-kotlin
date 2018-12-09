package com.dak.synacor

import java.util.*
import kotlin.math.pow

class SynacorVirtualMachine(private val program: MutableList<Int>) {
    private val registers = IntArray(10)
    private val stack: Deque<Int> = ArrayDeque()
    private val charsDeque: Deque<Char> = ArrayDeque()
    private val sc: Scanner = Scanner(System.`in`)
    private val operations: Map<OpCode, (Int) -> (Int)> = getOperations()

    init {
        // fill with noops until our address space is 2^15 (32768)
        if (program.size < 2 pow 15) {
            program.addAll(Collections.nCopies((2 pow 15) - program.size, OpCode.NOOP.opCode))
        }
    }

    fun execute() {
        var index = 0
        do {
            index = process(program, index)
        } while (index >= 0)
    }

    private fun getOperations(): Map<OpCode, (Int) -> (Int)> {
        val operations = mutableMapOf<OpCode, (Int) -> (Int)>()

        operations[OpCode.HALT] = { -1 }

        operations[OpCode.SET] = { startingIndex ->
            this.setRegister(program[startingIndex + 1], getValue(program[startingIndex + 2]))
            getIndexOfNextOpCode(program, startingIndex + OpCode.SET.numOfArguments + 1)
        }
        operations[OpCode.PUSH] = { startingIndex ->
            this.pushStack(getValue(program[startingIndex + 1]))
            getIndexOfNextOpCode(program, startingIndex + OpCode.PUSH.numOfArguments + 1)
        }

        operations[OpCode.POP] = { startingIndex ->
            this.setRegister(program[startingIndex + 1], getValue(this.popStack()))
            getIndexOfNextOpCode(program, startingIndex + OpCode.POP.numOfArguments + 1)
        }

        operations[OpCode.EQ] = { startingIndex ->
            this.setRegister(
                program[startingIndex + 1],
                if (getValue(program[startingIndex + 2]) == getValue(program[startingIndex + 3])) 1 else 0
            )
            getIndexOfNextOpCode(program, startingIndex + OpCode.EQ.numOfArguments + 1)
        }

        operations[OpCode.GT] = { startingIndex ->
            this.setRegister(
                program[startingIndex + 1],
                if (getValue(program[startingIndex + 2]) > getValue(program[startingIndex + 3])) 1 else 0
            )
            getIndexOfNextOpCode(program, startingIndex + OpCode.GT.numOfArguments + 1)
        }

        operations[OpCode.JMP] = { startingIndex -> process(program, getValue(program[startingIndex + 1])) }

        operations[OpCode.JT] = { startingIndex ->
            if (getValue(program[startingIndex + 1]) != 0)
                process(program, getValue(program[startingIndex + 2]))
            else
                getIndexOfNextOpCode(program, startingIndex + OpCode.JT.numOfArguments + 1)
        }

        operations[OpCode.JF] = { startingIndex ->
            if (getValue(program[startingIndex + 1]) == 0)
                process(program, getValue(program[startingIndex + 2]))
            else
                getIndexOfNextOpCode(program, startingIndex + OpCode.JF.numOfArguments + 1)
        }

        operations[OpCode.ADD] = { startingIndex ->
            this.setRegister(
                program[startingIndex + 1],
                getValue(program[startingIndex + 2]) + getValue(program[startingIndex + 3])
            )
            getIndexOfNextOpCode(program, startingIndex + OpCode.ADD.numOfArguments + 1)
        }

        operations[OpCode.MULT] = { startingIndex ->
            this.setRegister(
                program[startingIndex + 1],
                getValue(program[startingIndex + 2]) * getValue(program[startingIndex + 3])
            )
            getIndexOfNextOpCode(program, startingIndex + OpCode.MULT.numOfArguments + 1)
        }

        operations[OpCode.MOD] = { startingIndex ->
            this.setRegister(
                program[startingIndex + 1],
                getValue(program[startingIndex + 2]) % getValue(program[startingIndex + 3])
            )
            getIndexOfNextOpCode(program, startingIndex + OpCode.MOD.numOfArguments + 1)
        }

        operations[OpCode.AND] = { startingIndex ->
            this.setRegister(
                program[startingIndex + 1],
                getValue(program[startingIndex + 2]) and getValue(program[startingIndex + 3])
            )
            getIndexOfNextOpCode(program, startingIndex + OpCode.AND.numOfArguments + 1)
        }

        operations[OpCode.OR] = { startingIndex ->
            this.setRegister(
                program[startingIndex + 1],
                getValue(program[startingIndex + 2]) or getValue(program[startingIndex + 3])
            )
            getIndexOfNextOpCode(program, startingIndex + OpCode.OR.numOfArguments + 1)
        }

        operations[OpCode.NOT] = { startingIndex ->
            this.setRegister(program[startingIndex + 1], getValue(program[startingIndex + 2]).inv())
            getIndexOfNextOpCode(program, startingIndex + OpCode.NOT.numOfArguments + 1)
        }

        operations[OpCode.RMEM] = { startingIndex ->
            this.setRegister(program[startingIndex + 1], program[getValue(program[startingIndex + 2])])
            getIndexOfNextOpCode(program, startingIndex + OpCode.RMEM.numOfArguments + 1)
        }

        operations[OpCode.WMEM] = { startingIndex ->
            program[getValue(program[startingIndex + 1])] = getValue(program[startingIndex + 2])
            getIndexOfNextOpCode(program, startingIndex + OpCode.WMEM.numOfArguments + 1)
        }

        operations[OpCode.CALL] = { startingIndex ->
            this.pushStack(getIndexOfNextOpCode(program, startingIndex + 2))
            process(program, getValue(program[startingIndex + 1]))
        }

        operations[OpCode.RET] = { process(program, getValue(this.popStack())) }

        operations[OpCode.OUT] = { startingIndex ->
            print(Character.toString(getValue(program[startingIndex + 1]).toChar()))
            getIndexOfNextOpCode(program, startingIndex + OpCode.OUT.numOfArguments + 1)
        }

        operations[OpCode.IN] = { startingIndex ->
            val result = getNextChar().toInt()
            val target = program[startingIndex + 1]

            if (isRegisterReference(target)) {
                this.setRegister(target, result)
            } else {
                program[getValue(target)] = result
            }

            getIndexOfNextOpCode(program, startingIndex + OpCode.IN.numOfArguments + 1)
        }

        operations[OpCode.NOOP] = { startingIndex ->
            getIndexOfNextOpCode(program, startingIndex + OpCode.NOOP.numOfArguments + 1)
        }

        return operations
    }

    private fun setRegister(index: Int, value: Int) {
        registers[index % 32768] = if (value < 0) 32768 + value else value % 32768
    }

    private fun getIndexOfNextOpCode(program: List<Int>, startingIndex: Int): Int {
        for (i in startingIndex until program.size) {
            if (OpCode.isOpCode(program[i])) {
                return i
            }
        }

        return -1
    }

    private fun getNextChar(): Char {
        if (charsDeque.isEmpty()) {
            sc.nextLine().toCharArray().forEach {
                charsDeque.push(it)
            }

            charsDeque.push('\n')
        }

        return charsDeque.removeLast()
    }

    private fun process(program: List<Int>, startingIndex: Int): Int =
        operations[OpCode.fromInt(program[startingIndex])]!!.invoke(startingIndex)

    private fun pushStack(value: Int) = stack.push(value)
    private fun popStack(): Int = stack.pop()
    private fun getRegister(index: Int): Int = registers[index % 32768]
    private fun getValue(x: Int): Int = if (isRegisterReference(x)) this.getRegister(x) else x
    private fun isRegisterReference(candidate: Int): Boolean = candidate in (32768..32775)
    private infix fun Int.pow(exponent: Int): Int = toDouble().pow(exponent).toInt()
}
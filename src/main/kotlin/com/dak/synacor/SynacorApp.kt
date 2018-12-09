package com.dak.synacor

fun main() =
    SynacorVirtualMachine(getProgram().toMutableList()).execute()

private fun getProgram(): List<Int> =
    reverseEndianess(getFileAsBytes("/challenge.bin"))


private fun reverseEndianess(input: ByteArray): List<Int> =
    input
        .map(Byte::toUnsignedInt)
        .chunked(2)
        .map { (it.last() shl 8) + it.first() }

private fun Byte.toUnsignedInt() = java.lang.Byte.toUnsignedInt(this)

private fun getFileAsBytes(path: String): ByteArray =
    object {}.javaClass.getResource(path).readBytes()


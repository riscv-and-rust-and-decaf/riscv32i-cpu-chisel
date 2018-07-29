.global _start
_start:
    lui x1, 0x87654
    addi x2, x0, 0x1
    ori x1, x1, 0x321
    la x3, data
    sw x1, 1(x3)
    lw x2, 1(x3)
    addi x2, x2, 0x222

check:
    li x4, 0x87654321
    la a0, x1_wrong
    bne x1, x4, fail
    li x4, 0x87654543
    la a0, x2_wrong
    bne x2, x4, fail
pass:
    lui x31, 0xcafe
fail:
    lui x31, 0xdead
data:
    nop
    nop

x1_wrong: .string "x1 wrong\0"
x2_wrong: .string "x2 wrong\0"

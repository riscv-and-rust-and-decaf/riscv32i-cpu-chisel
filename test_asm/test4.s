.global _start
_start:
	li x10, 10
	li x2, 0
	li x1, 0
_SUM:
	addi x1, x1, 1
	add  x2, x2, x1
	bne  x1, x10, _SUM
check_x2:
	li x4, 55
    la a0, x2_wrong
    bne x4, x2, fail
test2:
	li x4, 1000
	li x5, 1
	li x6, 1
_FI:
	add x5, x5, x6
	bge x5,x4, _END5
	add x6, x5,x6
	bge x6,x4, _END6
	j _FI
_END5:
	mv x7, x5
	j _END
_END6:
	mv x7,x6
	j _END
_END:

check:
    li x8, 1000
    la a0, x4_wrong
    bne x4, x8, fail
    li x8, 1597
    la a0, x5_wrong
    bne x5, x8, fail
    li x8, 987
    la a0, x6_wrong
    bne x6, x8, fail
    li x8, 1597
    la a0, x7_wrong
    bne x7, x8, fail
pass:
	lui x31, 0xcafe
fail:
    lui x31, 0xdead

x2_wrong: .string "x2 != 55\0"
x4_wrong: .string "x4 wrong\0"
x5_wrong: .string "x5 wrong\0"
x6_wrong: .string "x6 wrong\0"
x7_wrong: .string "x7 wrong\0"
unknown:  .string "unknown reason fail\0"

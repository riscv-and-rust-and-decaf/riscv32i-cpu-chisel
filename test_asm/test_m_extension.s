.global _start
_start:
	li x1, 5
	li x2, 2
	div x3,x1,x2
	mul x4,x3,x2
	rem x5,x1,x2
check:
    li t0, 2
    la a0, div_wrong
    bne x3, t0, fail

    li t0, 4
    la a0, mul_wrong
    bne x4, t0, fail

    li t0, 1
    la a0, rem_wrong
    bne x5, t0, fail

pass:
	lui x31, 0xcafe
fail:
    lui x31, 0xdead

div_wrong: .string "div wrong\0"
mul_wrong: .string "mul wrong\0"
rem_wrong: .string "rem wrong\0"

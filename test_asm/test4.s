.global _start
_start:
	li x10, 10
	li x2, 0
	li x1, 0
_SUM:
	addi x1, x1, 1
	add  x2, x2, x1
	bne  x1, x10, _SUM
	mv x7, x2
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
	sub x1, x7, x4
	j _END
	nop

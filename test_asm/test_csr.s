.global _start
_start:
	li x1,13
	csrw mtvec,x1
	csrrwi x2,mtvec,11 # x2 should be 13, mtvec become 11
	li x15, 7
	csrrc x3,mtvec,x15 # x3 should be 11, mtvec become 8
	csrrsi x4,mtvec,2   # x4 should be 8
	csrr x5, mtvec     # x5 should be 10
	nop
	nop
	nop
	nop
	nop
	nop
	nop
	nop
	nop
	nop
	nop
	nop
	nop


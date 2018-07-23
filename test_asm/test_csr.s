_start:
	li x1,13
	nop
	nop
	nop
	nop
	csrw mtvec,x1
	nop
	nop
	nop
	nop
	csrrwi x2,mtvec,11 # x2 should be 13, mtvec become 11
	nop
	nop
	nop
	nop
	li x15, 7
	nop
	nop
	nop
	nop
	csrrc x3,mtvec,x15 # x3 should be 11, mtvec become 8
	nop
	nop
	nop
	nop
	csrrsi x4,mtvec,2   # x4 should be 8
	nop
	nop
	nop
	nop
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


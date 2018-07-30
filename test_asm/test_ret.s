.global _start
_start:
	li x30, 0
	j reset_vector

trap_vector:
	li x30, 7 
	li x2, 2
	li x3, 3
	add x5,x2,x3
	add x8,x5,x3
	add x11,x8,x3
epc_add4:
	csrr t0, mepc
	addi t0, t0, 4
	csrw mepc, t0
mret:
	mret
    la a0, mret_error
    j fail

reset_vector:
	la t0, trap_vector
	csrw   mtvec, t0
	csrsi  mstatus, 15
	ecall

check_ecall:
	li x20, 7
	la a0, ecall_error
	bne x30, x20, fail

check_mcause:
	csrr t0, mcause
    la a0, mcause_wrong
	bne x11, t0, fail
pass:
    lui x31, 0xcafe
fail:
    lui x31, 0xdead

mcause_wrong: .string "mcause wrong\0"
ecall_error:  .string "ecall error\0"
mret_error:   .string "mret error\0"

	

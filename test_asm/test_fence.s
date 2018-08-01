.global _start
_start:
	li x1, 5
	auipc x7,0
	lw x2, -4(x7)
	fence
	li x1, 6
	sfence.vma
	auipc x7,0
	sw x2, 12(x7)
	fence.i
	li x1, 7
end:
	j end
	nop
	nop
	




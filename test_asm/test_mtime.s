.global _start
_start:
	li a1, 100
	li a0, 200
	li t0, -1
	sw t0, mtimecmp
	sw a1, mtimecmp+4
	sw a0, mtimecmp


.global _start
_start:
    lui x1, 0x87654
    addi x2, x0, 0x1
    ori x1, x1, 0x321
    la x3, data
    sw x1, 1(x3)
    lw x2, 1(x3)
    addi x2, x2, 0x222
end:
    j end
data:
    nop
    nop

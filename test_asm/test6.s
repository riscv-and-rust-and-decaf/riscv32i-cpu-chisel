.global _start
_start:
    lui x1, 0xC0FFF
    addi x1, x1, -512
    addi x2, x0, 0x100
    addi x3, x0, 0x200

l1:
    sw x1, 0(x2)
    addi x2, x2, 4
    blt x2, x3, l1

    addi x2, x0, 0x100
    addi x3, x0, 0x200
l2:
    addi x29, x0, 0
    lw x29, 0(x2)
    addi x2, x2, 4
    blt x2, x3, l2

l3:
    j l3
    nop
    nop

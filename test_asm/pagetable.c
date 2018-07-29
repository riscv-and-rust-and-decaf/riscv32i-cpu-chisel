#include "serial.h"
#include "entry.h"

typedef u32 pte_t;

const pte_t PTE_V = 1 << 0;
const pte_t PTE_R = 1 << 1;
const pte_t PTE_W = 1 << 2;
const pte_t PTE_X = 1 << 3;
const pte_t PTE_VRWX = 0xf;
const pte_t PTE_VRW  = 0x7;

static inline pte_t
make_pte(u32 p2, u32 p1, u32 flags) {
    return (p2 << 20) | (p1 << 10) | flags;
}

pte_t* const page_root = (pte_t*)0x80001000;
pte_t* const page1     = (pte_t*)0x80002000;

void enable_page_table() {
//    for(int i=0; i<0x1000; ++i)
//        page_root[i] = page1[i] = 0;

    page_root[0x200] = make_pte(0x200, 0, PTE_VRWX);     // Map [0x200] to a huge page
    page_root[0x201] = make_pte(0x200, 1, PTE_VRW);      // Map [0x201] to a not aligned huge page
    page_root[0x3ff] = make_pte(0x200, 1, PTE_V);        // Map [0x3ff] to root as PDE
    page_root[0x3fe] = make_pte(0x200, 1, PTE_VRW);      // Map [0x3fe] to root as PTE
    page_root[0x040] = make_pte(0x200, 2, PTE_V);        // Map [0x040] to a next level page table
    page1[0x000]     = make_pte(0x040, 0, PTE_VRW);      // Map [0x040][0] to a page 0x10000 (Serial)
    page1[0x001]     = make_pte(0x040, 0, PTE_V);        // Map [0x040][1] to invalid

    puts("Enable paging\n");
    u32 satp = (1 << 31) | ((u32)page_root >> 12);
    asm volatile ("csrw satp, %0" : : "r"(satp) : "memory");
    asm volatile ("nop\nnop\nnop\nnop\n");
}

void check_paging() {
    pte_t* const virt_root = (pte_t*)0xffffe000;
    assert(virt_root[0x200] == page_root[0x200],
        "Failed to access root page table from 0xffffe000");

    virt_root[0x000] = make_pte(0x200, 0, PTE_VRWX);
    assert(page_root[0x000] == make_pte(0x200, 0, PTE_VRWX),
        "Failed to write root page table");

    assert(*((u32*)0) == *((u32*)0x80000000),
        "Failed to read data from new mapped page");
}

int main() {
    enable_page_table();
    puts("Hello page table!\n");
    check_paging();
}
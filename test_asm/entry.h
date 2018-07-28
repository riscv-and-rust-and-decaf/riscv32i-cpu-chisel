asm(
".section .text\n"
".global _start\n"
"_start:\n"
"li sp, 0x80001000\n"   // Set stack top
"j main\n");
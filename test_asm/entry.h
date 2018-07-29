asm(
".section .text\n"
".global _start\n"
"_start:\n"
"li sp, 0x80001000\n"   // Set stack top
"call main\n"
"j pass\n");

void pass();
void fail(const char* reason);

asm(
"pass:\n"
"lui x31, 0xcafe\n"
"fail:\n"
"lui x31, 0xdead\n"
);

static inline void
assert(int value, const char* explain) {
    if(!value) fail(explain);
}
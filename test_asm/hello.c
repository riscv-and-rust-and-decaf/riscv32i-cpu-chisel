typedef unsigned char u8;
typedef int bool;

volatile u8* const serial_data         = (u8*)0x10000000;
volatile const u8* const serial_status = (const u8*)0x10000005;
const int SERIAL_READ = 1 << 0;
const int SERIAL_WRITE = 1 << 5;

bool serial_can_read() {
    return *serial_status & SERIAL_READ;
}
bool serial_can_write() {
    return *serial_status & SERIAL_WRITE;
}

void putchar(char c) {
    while(!serial_can_write());
    *serial_data = c;
}

char getchar() {
    while(!serial_can_read());
    return *serial_data;
}

void puts(const char* s) {
    for(; *s; s++)
        putchar(*s);
}

asm(
".section .text\n"
".global _start\n"
"_start:\n"
"li sp, 0x80001000\n"   // Set stack top
"j main\n");

int main()  {
    puts("Hello world!\n");
    char c;
    while((c = getchar()))
        putchar(c);
}
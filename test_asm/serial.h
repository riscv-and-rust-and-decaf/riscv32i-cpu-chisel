typedef unsigned char u8;
typedef unsigned int u32;
typedef int bool;

volatile u8* const serial_data         = (u8*)0x10000000;
volatile const u8* const serial_status = (const u8*)0x10000005;
const int SERIAL_READ = 1 << 0;
const int SERIAL_WRITE = 1 << 5;

static inline bool serial_can_read() {
    return *serial_status & SERIAL_READ;
}
static inline bool serial_can_write() {
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
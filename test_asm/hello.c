#include "serial.h"
#include "entry.h"

int main()  {
    puts("Hello world!\n");
    char c;
    while((c = getchar()))
        putchar(c);
}
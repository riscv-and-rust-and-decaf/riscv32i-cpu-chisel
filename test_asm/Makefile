insts := $(patsubst %.s,%.hex,$(wildcard *.s)) \
         $(patsubst %.c,%.hex,$(wildcard *.c))

PREFIX := riscv64-unknown-elf-
OBJCOPY := ${PREFIX}objcopy
CC := ${PREFIX}gcc

CFLAGS := -mabi=ilp32 -march=rv32im -O2 -std=gnu99 -Wno-unused -fno-builtin -Wall -nostdinc -fno-stack-protector -ffunction-sections -fdata-sections -nostartfiles

all: ${insts} riscv-test

riscv-test:
	make -C riscv-test

%.hex: %.bin
	hexdump -v -e '1/1 "%02x\n"' $^ > $@

%.bin: %.o
	${OBJCOPY} -O binary $^ $@

%.o: %.c
	${CC} ${CFLAGS} $^ -o $@ -Tlinker.ld

%.o: %.s
	${CC} ${CFLAGS} $^ -o $@ -Tlinker.ld

clean:
	make -C riscv-test clean
	rm -f *.bin *.o *.run

.PHONY: all clean riscv-test

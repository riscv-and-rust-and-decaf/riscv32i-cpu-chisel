insts := $(patsubst %.s,%.bin,$(wildcard *.s)) \
         $(patsubst %.c,%.c.bin,$(wildcard *.c))

all: ${insts} monitor riscv-test

monitor:
	make -C monitor

riscv-test:
	make -C riscv-test

%.c.bin: %.c
	../tools/assembler $^

%.bin: %.s
	../tools/assembler $^

clean:
	make -C monitor clean
	make -C riscv-test clean
	rm -f *.bin *.o *.run

.PHONY: all clean monitor riscv-test

insts := $(patsubst %.s,%.bin,$(wildcard *.s)) \
         $(patsubst %.c,%.c.bin,$(wildcard *.c))

all: ${insts} monitor/monitor.bin

monitor/monitor.bin:
	make -C monitor

%.c.bin: %.c
	../tools/assembler $^

%.bin: %.s
	../tools/assembler $^

clean:
	make -C monitor clean
	rm *.bin
	rm *.o

.PHONY: all clean

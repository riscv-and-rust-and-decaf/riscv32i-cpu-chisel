insts := $(patsubst %.s,%.bin,$(wildcard *.s)) \
         $(patsubst %.c,%.c.bin,$(wildcard *.c))

all: ${insts}

%.c.bin: %.c
	../tools/assembler $^

%.bin: %.s
	../tools/assembler $^

clean:
	rm *.bin
	rm *.o

.PHONY: all clean

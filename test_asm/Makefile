insts := $(patsubst %.s,%.bin,$(wildcard *.s))

all: ${insts}

%.bin: %.s
	../tools/assembler $^

clean:
	rm *.bin
	rm *.o

.PHONY: all clean

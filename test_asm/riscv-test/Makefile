ARCH_PREF := riscv64-unknown-elf-
GCC := $(ARCH_PREF)gcc
LD := $(ARCH_PREF)ld
OBJDUMP := $(ARCH_PREF)objdump
OBJCOPY := $(ARCH_PREF)objcopy

TESTS := $(patsubst ./obj/%, %, $(wildcard ./obj/*))
DUMPS := $(patsubst %, %.dump, $(TESTS))
BINS := $(patsubst %, %.bin, $(TESTS))

all: $(DUMPS) $(BINS)

%.dump: ./obj/%
	$(OBJDUMP) -D $^ > $@

%.bin: ./obj/%
	$(OBJCOPY) -O binary $^ $@

clean:
	rm -f *.dump *.bin *.run

.PHONY: all clean


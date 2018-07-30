ARCH_PREF := riscv32-unknown-elf-
GCC := $(ARCH_PREF)gcc
LD := $(ARCH_PREF)ld
OBJDUMP := $(ARCH_PREF)objdump
OBJCOPY := $(ARCH_PREF)objcopy

TESTS := breakpoint csr illegal ma_addr ma_fetch mcsr sbreak scall shamt
OBJS := $(patsubst %, rv32mi-p-%, $(TESTS))
DUMPS := $(patsubst %, rv32mi-p-%.dump, $(TESTS))
BINS := $(patsubst %, rv32mi-p-%.bin, $(TESTS))

all: $(DUMPS) $(BINS)

%.dump: %
	$(OBJDUMP) -D $^ > $@

%.bin: %
	$(OBJCOPY) -O binary $^ $@

clean:
	rm -f *.dump *.bin *.run

.PHONY: all clean


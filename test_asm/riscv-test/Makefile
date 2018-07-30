ARCH_PREF := riscv32-unknown-elf-
GCC := $(ARCH_PREF)gcc
LD := $(ARCH_PREF)ld
OBJDUMP := $(ARCH_PREF)objdump
OBJCOPY := $(ARCH_PREF)objcopy

TESTS := $(patsubst %, rv32mi-p-%, breakpoint csr illegal ma_addr ma_fetch mcsr sbreak scall shamt)\
         $(patsubst %, rv32si-p-%, csr dirty ma_fetch sbreak scall wfi)
OBJS := $(patsubst %, %, $(TESTS))
DUMPS := $(patsubst %, %.dump, $(TESTS))
BINS := $(patsubst %, %.bin, $(TESTS))

all: $(DUMPS) $(BINS)

%.dump: %
	$(OBJDUMP) -D $^ > $@

%.bin: %
	$(OBJCOPY) -O binary $^ $@

clean:
	rm -f *.dump *.bin *.run

.PHONY: all clean


csrs = ["mvendorid","marchid","mimpid","mhartid","mstatus", "misa", "medeleg", "mideleg", "mie", "mtvec", "mcounteren", "mscratch", "mepc", "mcause", "mtval", "mip"]

for r in csrs:
    print("ADDR.%s -> %s," % (r,r))

print("---------")
for r in csrs:
    print("is(ADDR.%s) {%s := newVar}" % (r,r))


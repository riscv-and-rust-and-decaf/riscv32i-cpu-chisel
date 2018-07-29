csrs = ["mvendorid","marchid","mimpid","mhartid","mstatus", "misa", "medeleg", "mideleg", "mie", "mtvec", "mcounteren", "mscratch", "mepc", "mcause", "mtval", "mip","sepc","scause","uepc","ucause"]

for r in csrs:
    print("val %s = RegInit(0.U(32.W))" % r)

for r in csrs:
    print("ADDR.%s -> %s," % (r,r))

print("---------")
for r in csrs:
    print("is(ADDR.%s) {%s := newVar}" % (r,r))


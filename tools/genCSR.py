csrs = ["mstatus", "misa", "medeleg", "mideleg", "mie", "mtvec", "mcounteren", "mscratch", "mepc", "mcause", "mtval", "mip"]

for r in csrs:
    print("val %s = RegInit(0.U(32.W))"%r)

print("==================")

for r in csrs:
    print("is(ADDR.%s) {\nrsV := %s\n%s := newV\n}"%(r,r,r))


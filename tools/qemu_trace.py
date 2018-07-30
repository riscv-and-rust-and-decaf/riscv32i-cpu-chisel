import sys
import subprocess
import time
import io

USAGE = 'Usage: qemu_trace.py <kernel_obj>'

kernel = sys.argv[1]

p = subprocess.Popen([
	'qemu-system-riscv32',
	'-machine', 'virt',
	'-kernel', kernel, '-nographic',
	'-d', 'in_asm'], 
	stderr=subprocess.PIPE)

time.sleep(2)
p.kill()

trace_file = open(kernel + '.std.run', 'w')
for line in io.TextIOWrapper(p.stderr, encoding="utf-8"): 
	if line[0:2] == '0x':
		trace_file.write(line[2:10] + '\n')
trace_file.close()


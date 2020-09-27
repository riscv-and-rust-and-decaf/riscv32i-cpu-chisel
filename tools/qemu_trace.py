import sys
import subprocess
import io
import signal

class TimeoutError(Exception):
	pass

def handler(signum, frame):
	raise TimeoutError()

USAGE = """Usage: qemu_trace.py <kernel_obj>

Run a program in QEMU and dump its PCs."""

if len(sys.argv) <= 1:
	print(USAGE)
	exit(1)

kernel = sys.argv[1]

p = subprocess.Popen([
	'qemu-system-riscv32',
	'-machine', 'virt',
	'-bios', 'none',
	'-kernel', kernel, '-nographic',
	'-d', 'in_asm'], 
	stderr=subprocess.PIPE)

# set the timeout handler
signal.signal(signal.SIGALRM, handler)
signal.alarm(2)

try:
	with open(kernel + '.std.run', 'w') as trace_file:
		for line in io.TextIOWrapper(p.stderr, encoding="utf-8"):
			if line[0:2] == '0x':
				trace_file.write(line[2:10] + '\n')
except TimeoutError:
	p.kill()

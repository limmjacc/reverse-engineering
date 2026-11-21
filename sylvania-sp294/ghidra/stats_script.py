from ghidra.program.model.listing import CodeUnit

listing = currentProgram.getListing()
mem = currentProgram.getMemory()

total_bytes = 0
instr_bytes = 0
undefined_bytes = 0
instr_count = 0

addr = mem.getMinAddress()
max_addr = mem.getMaxAddress()

cu_iter = listing.getCodeUnits(True)
sample = []
count = 0
for cu in cu_iter:
    length = cu.getLength()
    total_bytes += length
    if cu.getMnemonicString() == "??":
        undefined_bytes += length
    else:
        instr_bytes += length
        if hasattr(cu, "getMnemonicString"):
            instr_count += 1
            if count < 40:
                sample.append("%s: %s" % (cu.getAddress(), cu.toString()))
                count += 1

print("TOTAL_BYTES=%d" % total_bytes)
print("INSTR_BYTES=%d" % instr_bytes)
print("UNDEFINED_BYTES=%d" % undefined_bytes)
print("INSTR_COUNT=%d" % instr_count)
print("--- sample disassembly ---")
for s in sample:
    print(s)

import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.CodeUnit;
import ghidra.program.model.listing.CodeUnitIterator;

public class StatsScript2 extends GhidraScript {
    @Override
    public void run() throws Exception {
        Address start = currentProgram.getMinAddress();
        println("Forcing disassembly at: " + start);
        try {
            disassemble(start);
        } catch (Exception e) {
            println("disassemble() threw: " + e);
        }

        long totalBytes = 0;
        long instrBytes = 0;
        long undefinedBytes = 0;
        long instrCount = 0;
        int sampleCount = 0;

        CodeUnitIterator it = currentProgram.getListing().getCodeUnits(true);
        while (it.hasNext()) {
            CodeUnit cu = it.next();
            int length = cu.getLength();
            totalBytes += length;
            String mnem = cu.getMnemonicString();
            if (mnem == null || mnem.equals("??")) {
                undefinedBytes += length;
            } else {
                instrBytes += length;
                instrCount++;
                if (sampleCount < 40) {
                    println(cu.getAddress() + ": " + cu.toString());
                    sampleCount++;
                }
            }
        }
        println("TOTAL_BYTES=" + totalBytes);
        println("INSTR_BYTES=" + instrBytes);
        println("UNDEFINED_BYTES=" + undefinedBytes);
        println("INSTR_COUNT=" + instrCount);
    }
}

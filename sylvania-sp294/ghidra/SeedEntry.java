import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;

public class SeedEntry extends GhidraScript {
    @Override
    public void run() throws Exception {
        Address start = currentProgram.getMinAddress();
        createFunction(start, "entry");
        disassemble(start);
    }
}

import java.io.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

// Class that contains the main method to run this program.
public class Project1 {
	
	// Main method to run this program.
	public static void main(String[] args) {
		
		if (args.length != 2) {
			System.err.println("Usage: <filename> <timeout>");
			return;
		}

		String filename = args[0];
		int delay = Integer.parseInt(args[1]); // Parse delay.
		Runtime runtime = Runtime.getRuntime();

		try {
			Process memory = runtime.exec("java Memory " + filename); // Execute Memory in
			// a separate process.

			BufferedReader reader = new BufferedReader(new InputStreamReader(memory.getInputStream()));
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(memory.getOutputStream()));
			CPU cpu = new CPU(reader, writer, delay);
			cpu.execute();
		} catch (IOException exc) {
			System.err.println("Fatal error occurred: " + exc);
			System.exit(0);
		}
	}
	
	//CPU
	// It will have these registers:  PC, SP, IR, AC, X, Y.
	static class CPU implements Instruction {

		private int PC; // Program counter.

		private int SP = TIMER_INTERRUPT; // Stack pointer (initially timer interrupt is set to 1000).

		private int IR; // Instruction register.

		private int AC; // Accumulator.

		private int X; // X instruction.

		private int Y; // Y instruction.

		private int timer; // Timer to measure time elapsed.

		private int delay; // Delay for timer interruption.

		private boolean kernelMode; // True if it's possible to execute any CPU instruction, 
		// otherwise false.

		private BufferedReader reader; // To reader from memory.

		private PrintWriter writer; // To write to memory.

		// Constructor of CPU class to instantiate new CPU and initialize
		// it with reader, writer and delay.
		public CPU(BufferedReader read, PrintWriter write, int delay) {
			this.reader = read;
			this.writer = write;
			this.delay = delay;
		}

		// Reads from memory using the given address.
		@Override
		public int readFromMemory(int addr) throws NumberFormatException, IOException {
			if (addr >= TIMER_INTERRUPT && !kernelMode) {
				System.err.println("Memory violation: accessing system address 1000 in user mode");
				System.exit(0);
			}
			writer.println(READ_COMMAND + "" + addr);
			writer.flush();
			return Integer.parseInt(reader.readLine());
		}

		// Writes address and info into memory.
		@Override
		public void writeToMemory(int addr, int info) {
			writer.println(WRITE_COMMAND + "" + addr + ":" + info);
			writer.flush();
		}

		// Executes fetching phase.
		public void execute() throws NumberFormatException, IOException {
			boolean run = true;
			do {
				IR = readFromMemory(PC ++);
				run = process();
				timer ++;
				if (timer >= delay) { // A timer interrupt should cause execution at address 1000.
					if (!kernelMode) { // Check for kernel mode (ability to execute any CPU instruction).
						timer = 0;
						applyKernel();
						PC = TIMER_INTERRUPT;
					}
				}
			} while (run);
		}

		// Pushes info to the stack.
		public void push(int info) {
			-- SP;
			writeToMemory(SP, info);
		}

		// Pops info from the stack.
		public int pop() throws NumberFormatException, IOException {
			return readFromMemory(SP ++);
		}

		// Here this method applies kernel mode, so it needs to toggle stacks along with
		// the storing SP (user settings), PC.
		public void applyKernel() {
			kernelMode = true;
			int spCopy = SP; // To prevent changing instance variable SP, it should only be
			// pushed onto stack, nothing more.
			
			SP = MAX_ENTRIES; // Reset SP.
			push(spCopy);
			push(PC);
			push(IR);
			push(AC);
			push(X);
			push(Y);
		}

		// Processes instructions.
		public boolean process() throws NumberFormatException, IOException {
			if (IR == LOAD_VALUE) {
				IR = readFromMemory(PC ++);
				AC = IR;
			} else if (IR == LOAD_ADDR) {
				IR = readFromMemory(PC ++);
				AC = readFromMemory(IR);
			} else if (IR == LOAD_IND_ADDR) {
				IR = readFromMemory(PC ++);
				AC = readFromMemory(readFromMemory(IR));
			} else if (IR == LOAD_ID_XX_ADDR) {
				IR = readFromMemory(PC ++);
				AC = readFromMemory(IR + X);
			} else if (IR == LOAD_ID_XY_ADDR) {
				IR = readFromMemory(PC ++);
				AC = readFromMemory(IR + Y);
			} else if (IR == LOAD_SP_X) {
				AC = readFromMemory(SP + X);
			} else if (IR == STORE_ADDR) {
				IR = readFromMemory(PC ++);
				writeToMemory(IR, AC);
			} else if (IR == GET) {
				AC = ThreadLocalRandom.current().nextInt(1, 101);
			} else if (IR == PUT_PORT) {
				IR = readFromMemory(PC ++);
				if (IR == 1) {
					System.out.print(AC);
				} else if (IR == 2) {
					System.out.print((char) AC);
				}
			} else if (IR == ADD_X) {
				AC += X;
			} else if (IR == ADD_Y) {
				AC += Y;
			} else if (IR == SUB_X) {
				AC -= X;
			} else if (IR == SUB_Y) {
				AC -= Y;
			} else if (IR == COPY_TO_X) {
				X = AC;
			} else if (IR == COPY_FROM_X) {
				AC = X;
			} else if (IR == COPY_TO_Y) {
				Y = AC;
			} else if (IR == COPY_FROM_Y) {
				AC = Y;
			} else if (IR == COPY_TO_SP) {
				SP = AC;
			} else if (IR == COPY_FROM_SP) {
				AC = SP;
			} else if (IR == JUMP_ADDR) {
				IR = readFromMemory(PC ++);
				PC = IR;
			} else if (IR == JUMP_IF_EQUAL_ADDR) {
				IR = readFromMemory(PC ++);
				if (AC == 0) {
					PC = IR;
				}
			} else if (IR == JUMP_IF_NOT_EQUAL_ADDR) {
				IR = readFromMemory(PC ++);
				if (AC != 0) {
					PC = IR;
				}
			} else if (IR == CALL_ADDR) {
				IR = readFromMemory(PC ++);
				push(PC);
				PC = IR;
			} else if (IR == RET) {
				PC = pop();
			} else if (IR == INC_X) {
				X ++;
			} else if (IR == DEC_X) {
				X --;
			} else if (IR == PUSH) {
				push(AC);
			} else if (IR == POP) {
				AC = pop();
			} else if (IR == INT) {
				if (kernelMode) {
					// Nothing to do in kernel mode, it doesn't make sense.
				} else {
					applyKernel();
					PC = TIMER_EXECUTION;
				}
			} else if (IR == I_RET) {
				Y = pop();
				X = pop();
				AC = pop();
				IR = pop();
				PC = pop();
				SP = pop();
				kernelMode = false;
			} else if (IR == END) { // The program ends when the End instruction is executed.
				writer.println(END_COMMAND);
				writer.flush();
				return false;
			} else {
				System.err.println("Cannot process command: error occurred (instruction is not valid): " + IR);
				writer.println(END_COMMAND);
				writer.flush();
				return false;
			}
			return true;
		}

	}
	
	// This interface defines constants and behavior for
	// CPU and Memory classes.
	public interface Instruction {

		public static final int LOAD_VALUE = 1; // Load the value into the AC.

		public static final int LOAD_ADDR = 2; // Load the value at the address into the AC.

		public static final int LOAD_IND_ADDR = 3; // Load the value from the address found in 
		// the given address into the AC (for example, if LoadInd 500, and 500 contains 100, 
		// then load from 100).

		public static final int LOAD_ID_XX_ADDR = 4; // Load the value at (address+X) into the AC
		// (for example, if LoadIdxX 500, and X contains 10, then load from 510).

		public static final int LOAD_ID_XY_ADDR = 5; // Load the value at (address+Y) into the AC. 	

		public static final int LOAD_SP_X = 6; // Load from (Sp+X) into the AC (if SP is 990, and 
		// X is 1, load from 991).

		public static final int STORE_ADDR = 7; // Store the value in the AC into the address.

		public static final int GET = 8; // Gets a random int from 1 to 100 into the AC.

		public static final int PUT_PORT = 9; // If port=1, writes AC as an int to the screen
		// If port=2, writes AC as a char to the screen

		public static final int ADD_X = 10; // Add the value in X to the AC.

		public static final int ADD_Y = 11; // Add the value in Y to the AC.

		public static final int SUB_X = 12; // Subtract the value in X from the AC.

		public static final int SUB_Y = 13; // Subtract the value in Y from the AC.

		public static final int COPY_TO_X = 14; // Copy the value in the AC to X.

		public static final int COPY_FROM_X = 15; // Copy the value in X to the AC.

		public static final int COPY_TO_Y = 16; // Copy the value in the AC to Y.

		public static final int COPY_FROM_Y = 17; // Copy the value in Y to the AC.

		public static final int COPY_TO_SP = 18; // Copy the value in AC to the SP.

		public static final int COPY_FROM_SP = 19; // Copy the value in SP to the AC.

		public static final int JUMP_ADDR = 20; // Jump to the address.

		public static final int JUMP_IF_EQUAL_ADDR = 21; // Jump to the address only 
		// if the value in the AC is zero.

		public static final int JUMP_IF_NOT_EQUAL_ADDR = 22; // Jump to the address only if the 
		// value in the AC is not zero.

		public static final int CALL_ADDR = 23; // Push return address onto stack, jump to the address.

		public static final int RET = 24; // Pop return address from the stack, jump to the address.

		public static final int INC_X = 25; // Increment the value in X.

		public static final int DEC_X = 26; // Decrement the value in X.

		public static final int PUSH = 27; // Push AC onto stack.

		public static final int POP = 28; // Pop from stack into AC.

		public static final int INT = 29; // Perform system call.

		public static final int I_RET = 30; // Return from system call.

		public static final int END = 50; // End execution.
		
		public static final int TIMER_INTERRUPT = 1000; // A timer interrupt should cause execution 
		// at address 1000.
		
		public static final int TIMER_EXECUTION = 1500; // The int instruction should cause 
		// execution at address 1500.
		
		public static final int MAX_ENTRIES = 2000; // It will consist of 2000 integer entries.
		
		public static final char READ_COMMAND = 'r'; // Stores read command.
		
		public static final char WRITE_COMMAND = 'w'; // Stores write command.
		
		public static final char END_COMMAND = 'e'; // Stores end command.
		
		// Reads from memory.
		public int readFromMemory(int addr) throws NumberFormatException, IOException;
		
		// Writes to memory.
		public void writeToMemory(int addr, int info);
	}

}
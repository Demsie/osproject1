import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

// Memory class.
// It will consist of 2000 integer entries, 0-999 for the user program, 1000-1999 for system code
// read(address) -  returns the value at the address
// write(address, data) - writes the data to the address
// Memory will initialize itself by reading a program file.

 public class Memory implements Project1.Instruction {

	private int[] data; // Stores memory data.

	// initialize it with the filename.
	public Memory(String filename) throws FileNotFoundException {
		data = new int[MAX_ENTRIES];

		try (Scanner sc = new Scanner(new File(filename))) {
			int i = 0;
			while (sc.hasNextLine()) { // Loop through the lines in the input file.

				String nextLine = sc.nextLine().trim();
				if (nextLine.isEmpty()) { // Ignore empty lines.
					continue;
				} else if (nextLine.startsWith(".")) { // Dot case.
					String afterDot = nextLine.substring(1);
					String items[] = afterDot.split("\\s+");
					i = Integer.parseInt(items[0]);
					continue;
				} else if (!Character.isDigit(nextLine.charAt(0))) { // Ignore not a number.
					continue;
				}

				String[] items = nextLine.split("\\s+");
				if (items.length <= 0) { // Ignore empty data.
					continue;
				} else {
					data[i ++] = Integer.parseInt(items[0]);
				}
			}
		}
	}

	// Main method to run Memory in a separate process.
	public static void main(String[] args) {

		if (args.length < 1) {
			System.err.println("Usage: <input filename>");
		} else {
			String filename = args[0];
			Memory mem;
			try {
				mem = new Memory(filename);
				mem.build();
			} catch (FileNotFoundException e) {
				System.err.println("Error occurred: " + e);
			}
			
		}
	}

	// Implementation of method to read from memory in Memory class (polymorphism).
	@Override
	public int readFromMemory(int addr) {
		return data[addr];
	}

	// Implementation of method to write to memory in Memory class (polymorphism).
	@Override
	public void writeToMemory(int addr, int info) {
		this.data[addr] = info;
	}
	
	// Builds Memory by reading the input file (r - read, w - write, e - end).
	public void build() {
		int addr, info;
		try (Scanner sc = new Scanner(System.in)) {
			while (sc.hasNextLine()) {
				String nextLine = sc.nextLine().trim();
				if (nextLine.isEmpty()) {
					break;
				}
				char inputInstruction = nextLine.charAt(0);
				
				if (inputInstruction == READ_COMMAND) {
					addr = Integer.parseInt(nextLine.substring(1));
					System.out.println(readFromMemory(addr));
				} else if (inputInstruction == WRITE_COMMAND) {
					String[] params = nextLine.substring(1).split(":");
					addr = Integer.parseInt(params[0]);
					info = Integer.parseInt(params[1]);
					writeToMemory(addr, info);
				} else if (inputInstruction == END_COMMAND) {
					System.exit(0); // The end, exits the program.
				}
			}
		}
	}

}
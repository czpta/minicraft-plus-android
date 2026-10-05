package awtshim.image;
public class DataBufferInt extends DataBuffer {
	private final int[] data;
	public DataBufferInt(int[] d) { data = d; }
	public int[] getData() { return data; }
}

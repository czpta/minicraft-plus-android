package awtshim.image;
public class DataBufferByte extends DataBuffer {
	private final byte[] data;
	public DataBufferByte(byte[] d) { data = d; }
	public byte[] getData() { return data; }
}

package awtshim.image;
public class WritableRaster {
	private final DataBuffer buf;
	public WritableRaster(DataBuffer b) { buf = b; }
	public DataBuffer getDataBuffer() { return buf; }
}

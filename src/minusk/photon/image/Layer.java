package minusk.photon.image;

import java.nio.ByteBuffer;

import static org.lwjgl.system.jemalloc.JEmalloc.*;

/**
 * Created by MinusKelvin on 17/09/15.
 */
public class Layer {
	public final ByteBuffer colorData;
	
	public final Document parent;
	
	public Layer(Document parent) {
		this.parent = parent;
		
		colorData = je_calloc(parent.getWidth()*parent.getHeight(), 4);
	}
	
	public BlendMode getBlendMode() {
		return BlendMode.getDefault();
	}
	
	public void dispose() {
		je_free(colorData);
	}
}

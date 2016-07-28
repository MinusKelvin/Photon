package minusk.photon.file.loaders;

import minusk.photon.file.ImageLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.lwjgl.system.jemalloc.JEmalloc.*;

/**
 * Created by MinusKelvin on 4/13/16.
 */
public class ImageIOLoader extends ImageLoader {
	private ByteBuffer contents;
	private final int width, height;
	
	public ImageIOLoader(File file) throws IOException {
		BufferedImage img = ImageIO.read(file);
		System.out.println(img.getType());
		width = img.getWidth();
		height = img.getHeight();
		contents = je_malloc(width*height*4);
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				contents.putInt((j + i*width)*4, img.getRGB(j,i));
			}
		}
	}
	
	@Override
	public int getLayerCount() {
		return 1;
	}
	
	@Override
	public int getWidth() {
		return width;
	}
	
	@Override
	public int getHeight() {
		return height;
	}
	
	@Override
	public ByteBuffer getNextLayer() {
		return contents;
	}
	
	@Override
	public void finish() {
		contents.position(0);
		je_free(contents);
	}
}

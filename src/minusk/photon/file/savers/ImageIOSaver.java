package minusk.photon.file.savers;

import minusk.photon.file.ImageSaver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author MinusKelvin
 */
public class ImageIOSaver extends ImageSaver {
	private final File file;
	private BufferedImage img;
	
	public ImageIOSaver(File file) throws IOException {
		this.file = file;
	}
	
	@Override
	public boolean isLayered() {
		return false;
	}
	
	@Override
	public void data(int width, int height, int layers) {
		img = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
		if (layers != 1)
			throw new IllegalStateException("layers != 1");
	}
	
	@Override
	public void writeLayer(ByteBuffer data) {
		for (int i = 0; i < img.getHeight(); i++) {
			for (int j = 0; j < img.getWidth(); j++) {
				img.setRGB(j,i, data.getInt((i*img.getWidth()+j)*4));
			}
		}
	}
	
	@Override
	public void finish() throws IOException {
		ImageIO.write(img, "PNG", file);
	}
}

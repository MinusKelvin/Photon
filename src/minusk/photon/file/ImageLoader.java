package minusk.photon.file;

import javafx.scene.control.Alert;
import minusk.photon.Photon;
import minusk.photon.file.loaders.ImageIOLoader;
import minusk.photon.image.Document;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by MinusKelvin on 4/13/16.
 */
public abstract class ImageLoader {
	private static HashMap<String, ImageLoaderFactory> imageLoaders = new HashMap<>();
	
	private static ImageLoader getImageLoader(File file, String extension) {
		if (imageLoaders.containsKey(extension)) {
			try {
				return imageLoaders.get(extension).get(file);
			} catch (Exception e) {
				Photon.get().logError(e);
				Photon.get().alert(Alert.AlertType.ERROR, "There was an error loading the image. See " +
						Photon.get().getErrorFilePath() + " for more information.");
				return null;
			}
		}
		Photon.get().logError("Tried to load an image with an unsupported type: "+extension);
		Photon.get().alert(Alert.AlertType.ERROR, "This image format ("+extension+") is not supported.");
		return null;
	}
	
	public static Document loadImage(File file) {
		String name = file.getName();
		int index = -1;
		for (int i = name.length()-1; i >= 0; i--) {
			if (name.charAt(i) == '.') {
				index = i;
				break;
			}
		}
		String extension = index == -1 ? "" : name.substring(index+1);
		
		try {
			ImageLoader loader = getImageLoader(file, extension);
			if (loader == null)
				return null;
			
			Document doc = new Document(loader.getWidth(), loader.getHeight());
			doc.file = file;
			int l = doc.getLayerCount();
			for (int i = l; i < loader.getLayerCount(); i++)
				doc.addLayer(i);
			
			for (int i = 0; i < loader.getLayerCount(); i++) {
				ByteBuffer src = loader.getNextLayer();
				ByteBuffer dst = doc.getLayer(i).colorData;
				if (src.remaining() != dst.remaining()) {
					Photon.get().logError("Buffers have differing lengths. Source: " + src.remaining() + " Destination: " + dst.remaining());
					Photon.get().logError("ImageLoader class: " + loader.getClass().getName());
					Photon.get().logError("File extension: " + extension);
					Photon.get().alert(Alert.AlertType.ERROR, "There was an error loading the image. See " +
							Photon.get().getErrorFilePath() + " for more information.");
					return null;
				}
				dst.put(src);
				src.position(0);
				dst.position(0);
			}
			
			return doc;
		} catch (Exception e) {
			Photon.get().logError("Error loading image of type "+extension+".");
			Photon.get().logError(e);
			Photon.get().alert(Alert.AlertType.ERROR, "There was an error loading the image. See " +
					Photon.get().getErrorFilePath() + " for more information.");
			return null;
		}
	}
	
	public static void initialize() {
		imageLoaders.put("png", ImageIOLoader::new);
	}
	
	public static Set<String> getLoadableFormats() {
		return imageLoaders.keySet();
	}
	
	public interface ImageLoaderFactory {
		ImageLoader get(File file) throws IOException;
	}
	
	public abstract int getLayerCount();
	public abstract int getWidth();
	public abstract int getHeight();
	/** The contents of the returned <code>ByteBuffer</code> must be copied, as the returned ByteBuffer may be reused. Format is BGRA */
	public abstract ByteBuffer getNextLayer();
	/** Must be called to clean up any resources used by this <code>ImageLoader</code>. */
	public abstract void finish();
}

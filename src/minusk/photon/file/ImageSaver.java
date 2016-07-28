package minusk.photon.file;

import javafx.scene.control.Alert;
import minusk.photon.Photon;
import minusk.photon.file.savers.ImageIOSaver;
import minusk.photon.image.Document;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by MinusKelvin on 4/14/16.
 */
public abstract class ImageSaver {
	private static HashMap<String, ImageSaverFactory> imageSavers = new HashMap<>();
	
	private static ImageSaver getImageSaver(File file, String extension) {
		if (imageSavers.containsKey(extension)) {
			try {
				return imageSavers.get(extension).get(file);
			} catch (Exception e) {
				Photon.get().logError(e);
				Photon.get().alert(Alert.AlertType.ERROR, "There was an error saving the image. See " +
						Photon.get().getErrorFilePath() + " for more information.");
				return null;
			}
		}
		Photon.get().logError("Tried to save an image with an unsupported type: "+extension);
		Photon.get().alert(Alert.AlertType.ERROR, "This image format ("+extension+") is not supported.");
		return null;
	}
	
	public static void saveImage(Document document, File file) {
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
			ImageSaver saver = getImageSaver(file, extension);
			if (saver == null)
				return;
			
			if (!saver.isLayered() && document.getLayerCount() != 1) {
				Photon.get().logError("Cannot save layered image in non-layered format. Layers: " + document.getLayerCount());
				Photon.get().alert(Alert.AlertType.ERROR, "Tried saving layered image in a non-layered format. This is not supported yet.");
			} else {
				saver.data(document.getWidth(), document.getHeight(), document.getLayerCount());
				for (int i = 0; i < document.getLayerCount(); i++) {
					saver.writeLayer(document.getLayer(i).colorData);
				}
				saver.finish();
			}
		} catch (Exception e) {
			Photon.get().logError("Error saving image of type "+extension+".");
			Photon.get().logError(e);
			Photon.get().alert(Alert.AlertType.ERROR, "There was an error saving the image. See " +
					Photon.get().getErrorFilePath() + " for more information.");
		}
	}
	
	public static void initialize() {
		imageSavers.put("png", ImageIOSaver::new);
	}
	
	public static Set<String> getSaveableFormats() {
		return imageSavers.keySet();
	}
	
	public interface ImageSaverFactory {
		ImageSaver get(File file) throws IOException;
	}
	
	public abstract boolean isLayered();
	public abstract void data(int width, int height, int layers);
	/** ByteBuffer cannot be written to. Format is BGRA. Layers written from bottom to top. */
	public abstract void writeLayer(ByteBuffer data);
	/** Must be called to clean up any resources used by this <code>ImageSaver</code>. */
	public abstract void finish() throws IOException;
}

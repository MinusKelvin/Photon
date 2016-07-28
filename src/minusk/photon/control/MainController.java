package minusk.photon.control;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import minusk.photon.file.ImageLoader;
import minusk.photon.file.ImageSaver;
import minusk.photon.image.BlendMode;
import minusk.photon.image.Document;
import minusk.photon.jfx.Display;
import org.lwjgl.opengl.GL;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_BGRA;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.system.jemalloc.JEmalloc.je_free;
import static org.lwjgl.system.jemalloc.JEmalloc.je_malloc;

public class MainController {
	private long glcontext;
	private WritableImage display;
	private Display displayer;
	private Document currentDoc;
	private Stage stage;
	private boolean panning = false;
	private double panStartX, panStartY;
	
	@FXML
	private void newFile(ActionEvent event) {
		System.out.println("new");
	}
	
	@FXML
	private void open(ActionEvent event) {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileNameExtensionFilter("Supported image formats",getSaveLoadFormats()));
//		chooser.addChoosableFileFilter(new FileNameExtensionFilter("Importable image formats",getImportableFormats()));
		int result = chooser.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
			Document doc = ImageLoader.loadImage(chooser.getSelectedFile());
			if (doc != null) {
				currentDoc = doc;
				stage.setTitle("Photon - "+currentDoc.file.getName());
				draw();
			}
		}
	}
	
	private String[] getSaveLoadFormats() {
		Set<String> loadExtensions = ImageLoader.getLoadableFormats();
		Set<String> saveExtensions = ImageSaver.getSaveableFormats();
		Set<String> set = new HashSet<>(loadExtensions);
//		set.retainAll(saveExtensions);
		return set.toArray(new String[set.size()]);
	}
	
	@FXML
	private void save(ActionEvent event) {
		if (currentDoc.file == null)
			saveAs(event);
		else {
			ImageSaver.saveImage(currentDoc, currentDoc.file);
		}
	}
	
	@FXML
	private void saveAs(ActionEvent event) {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileNameExtensionFilter("Supported image formats",getSaveLoadFormats()));
//		chooser.addChoosableFileFilter(new FileNameExtensionFilter("Exportable image formats",getExportableFormats()));
		int result = chooser.showSaveDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
			currentDoc.file = chooser.getSelectedFile();
			stage.setTitle("Photon - "+currentDoc.file.getName());
			save(event);
		}
	}
	
	@FXML
	public void quit(ActionEvent event) {
		glfwTerminate();
		
		stage.close();
	}
	
	private Stage colorPicker;
	
	@FXML
	public void colorPicker(ActionEvent event) {
		if (colorPicker.isShowing())
			colorPicker.close();
		else
			colorPicker.show();
	}
	
	public void setColorPicker(Stage colorPicker) {
		this.colorPicker = colorPicker;
	}
	
	private void draw() {
		if (getDisplayWidth() != 0 && getDisplayHeight() != 0) {
			if (display == null || (int) display.getWidth() != getDisplayWidth() || (int) display.getHeight() != getDisplayHeight())
				display = new WritableImage(getDisplayWidth(), getDisplayHeight());
			currentDoc.render();
			glBindTexture(GL_TEXTURE_2D, Document.getFinalTex());
			IntBuffer buf = je_malloc(4*getDisplayWidth()*getDisplayHeight()).asIntBuffer();
			glGetTexImage(GL_TEXTURE_2D, 0, GL_BGRA, GL_UNSIGNED_BYTE, buf);
			display.getPixelWriter().setPixels(0,0,getDisplayWidth(),getDisplayHeight(), PixelFormat.getIntArgbInstance(), buf, getDisplayWidth());
			buf.position(0);
			je_free(buf);
			GraphicsContext gc = displayer.displayer.getGraphicsContext2D();
			gc.drawImage(display, 0, 0);
		}
	}
	
	public void initialize(Node root, Stage stage) {
		instance = this;
		this.stage = stage;
		
		displayer = (Display) root.lookup("#display");
		displayer.resize = this::draw;
		displayer.onMouseDraggedProperty().bindBidirectional(displayer.onMouseMovedProperty());
		displayer.setOnMouseMoved(event -> {
			if (panning) {
				currentDoc.x -= currentDoc.toImageX(event.getX()) - panStartX;
				currentDoc.y -= currentDoc.toImageY(displayer.displayer.getHeight()-event.getY()) - panStartY;
			} else
				currentDoc.currentTool.mouseMove(currentDoc.toImageX(event.getX()), currentDoc.toImageY(displayer.displayer.getHeight()-event.getY()));
			draw();
		});
		displayer.setOnMousePressed(event -> {
			if (event.getButton() == MouseButton.MIDDLE) {
				panning = true;
				panStartX = currentDoc.toImageX(event.getX());
				panStartY = currentDoc.toImageY(displayer.displayer.getHeight()-event.getY());
			} else
				currentDoc.currentTool.mouseDown(currentDoc.toImageX(event.getX()),
						currentDoc.toImageY(displayer.displayer.getHeight()-event.getY()), event.getButton()==MouseButton.PRIMARY);
			draw();
		});
		displayer.setOnMouseReleased(event -> {
			if (event.getButton() == MouseButton.MIDDLE)
				panning = false;
			else
				currentDoc.currentTool.mouseUp(currentDoc.toImageX(event.getX()),
						currentDoc.toImageY(displayer.displayer.getHeight()-event.getY()), event.getButton()==MouseButton.PRIMARY);
			draw();
		});
		displayer.setOnScroll(event -> {
			if (event.getDeltaY() < 0)
				currentDoc.zoom /= event.getDeltaY()/-30;
			else if (event.getDeltaY() > 0)
				currentDoc.zoom *= event.getDeltaY()/30;
			draw();
		});
		
		glfwInit();
		
		glfwWindowHint(GLFW_VISIBLE, 0);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, 1);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
		
		glcontext = glfwCreateWindow(1,1,"",0,0);
		glfwMakeContextCurrent(glcontext);
		GL.createCapabilities();
		
		glBindVertexArray(glGenVertexArrays());
		
		Document.initialize();
		BlendMode.initialize();
		ImageLoader.initialize();
		ImageSaver.initialize();
		
		currentDoc = new Document(800,600);
	}
	
	public int getDisplayHeight() {
		return (int) displayer.displayer.getHeight();
	}
	
	public int getDisplayWidth() {
		return (int) displayer.displayer.getWidth();
	}
	
	public Document getCurrentDocument() {
		return currentDoc;
	}
	
	private static MainController instance;
	public static MainController get() {
		return instance;
	}
}

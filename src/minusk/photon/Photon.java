package minusk.photon;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import minusk.photon.control.ColorPicker;
import minusk.photon.control.MainController;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

public class Photon extends Application {
	private static Photon photon;
	
	private PrintStream errorOutput;
	private File errorFile;
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		{
			FXMLLoader loader = new FXMLLoader(getClass().getResource("res/scenes/main.fxml"));
			Parent root = loader.load();
			MainController controller = loader.getController();
			controller.initialize(root, primaryStage);
			primaryStage.setTitle("Photon");
			primaryStage.setScene(new Scene(root, 1024, 720));
			primaryStage.setOnCloseRequest((event) -> {
				event.consume();
				controller.quit(null);
			});
			primaryStage.show();
		}
		
		{
			FXMLLoader loader = new FXMLLoader(getClass().getResource("res/scenes/colorpicker.fxml"));
			Parent root = loader.load();
			ColorPicker controller = loader.getController();
			Stage stage = new Stage(StageStyle.UTILITY);
			controller.initialize(root, stage);
			stage.initOwner(primaryStage);
			stage.setTitle("Color Picker");
			stage.setScene(new Scene(root));
			stage.setResizable(false);
			stage.show();
			MainController.get().setColorPicker(stage);
		}
		
		photon = this;
	}
	
	public void logError(String err) {
		if (errorOutput == null)
			openErrorFile();
		
		errorOutput.print("[ERROR] ");
		errorOutput.println(err);
	}
	
	public void logError(Throwable err) {
		logError("[ERROR] An exception occurred:");
		err.printStackTrace(errorOutput);
	}
	
	public void logWarning(String warn) {
		if (errorOutput == null)
			openErrorFile();
		
		errorOutput.print("[WARN] ");
		errorOutput.println(warn);
	}
	
	private void openErrorFile() {
		assert errorOutput == null;
		assert errorFile == null;
		
		do {
			int time = (int) (Math.random() * 65536);
			errorFile = new File(System.getProperty("user.home") + "/.photon/errors/" + Integer.toHexString(time));
		} while (errorFile.exists());
		try {
			errorOutput = new PrintStream(errorFile);
		} catch (FileNotFoundException e) {
			alert(Alert.AlertType.ERROR, "A fatal error has occurred: The error log file " +
					getErrorFilePath() + " can't be written to.");
			System.exit(1);
		}
	}
	
	public String getErrorFilePath() {
		return errorFile.getAbsolutePath();
	}
	
	public void alert(Alert.AlertType type, String message) {
		Alert dialog = new Alert(type, message);
		// Fix dialog size
		dialog.getDialogPane().getChildren().stream()
				.filter(node -> node instanceof Label)
				.forEach(node -> ((Label)node).setMinHeight(Region.USE_PREF_SIZE));
		dialog.showAndWait();
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	public static Photon get() {
		return photon;
	}
}

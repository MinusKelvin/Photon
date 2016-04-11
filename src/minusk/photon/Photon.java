package minusk.photon;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import minusk.photon.control.ColorPicker;
import minusk.photon.control.MainController;

public class Photon extends Application {
	private static Photon photon;
	
	@Override
	public void start(Stage primaryStage) throws Exception{
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
		}
		
		photon = this;
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	public static Photon get() {
		return photon;
	}
}

package minusk.photon.control;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Parent;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.stage.Stage;
import minusk.photon.jfx.ColorDisplay;

/**
 * Created by MinusKelvin on 4/9/16.
 */
public class ColorPicker {
	private IntegerProperty primaryColor = new SimpleIntegerProperty();
	private IntegerProperty secondaryColor = new SimpleIntegerProperty();
	private IntegerProperty currentEditColor = new SimpleIntegerProperty();
	
	public void initialize(Parent root, Stage stage) {
		instance = this;
		ColorDisplay colorDisplay = (ColorDisplay) root.lookup("#colorDisplay");
		colorDisplay.primaryColorProperty().bindBidirectional(primaryColor);
		colorDisplay.secondaryColorProperty().bindBidirectional(secondaryColor);
		colorDisplay.primarySelectedProperty().addListener(((observable, oldValue, newValue) -> {
			if (oldValue)
				currentEditColor.unbindBidirectional(primaryColor);
			else
				currentEditColor.unbindBidirectional(secondaryColor);
			
			if (newValue)
				currentEditColor.bindBidirectional(primaryColor);
			else
				currentEditColor.bindBidirectional(secondaryColor);
		}));
		
		Slider redSlider = (Slider) root.lookup("#RSlider");
		currentEditColor.addListener((observable, oldValue, newValue) -> redSlider.setValue(newValue.intValue() & 0xff));
		redSlider.valueProperty().addListener((observable, oldValue, newValue) -> currentEditColor.set((currentEditColor.get() & ~0xff) | newValue.intValue()));
		
		Slider greenSlider = (Slider) root.lookup("#GSlider");
		currentEditColor.addListener((observable, oldValue, newValue) -> greenSlider.setValue(newValue.intValue() >> 8 & 0xff));
		greenSlider.valueProperty().addListener((observable, oldValue, newValue) -> currentEditColor.set((currentEditColor.get() & ~0xff00) | newValue.intValue() << 8));
		
		Slider blueSlider = (Slider) root.lookup("#BSlider");
		currentEditColor.addListener((observable, oldValue, newValue) -> blueSlider.setValue(newValue.intValue() >> 16 & 0xff));
		blueSlider.valueProperty().addListener((observable, oldValue, newValue) -> currentEditColor.set((currentEditColor.get() & ~0xff0000) | newValue.intValue() << 16));
		
		Slider alphaSlider = (Slider) root.lookup("#ASlider");
		currentEditColor.addListener((observable, oldValue, newValue) -> alphaSlider.setValue(newValue.intValue() >> 24 & 0xff));
		alphaSlider.valueProperty().addListener((observable, oldValue, newValue) -> currentEditColor.set((currentEditColor.get() & ~0xff000000) | newValue.intValue() << 24));
		
		Spinner<Integer> redSpinner = (Spinner) root.lookup("#RSpinner");
		currentEditColor.addListener((observable, oldValue, newValue) -> redSpinner.getValueFactory().setValue(newValue.intValue() & 0xff));
		redSpinner.valueProperty().addListener((observable, oldValue, newValue) -> currentEditColor.set((currentEditColor.get() & ~0xff) | newValue));
		
		Spinner<Integer> greenSpinner = (Spinner) root.lookup("#GSpinner");
		currentEditColor.addListener((observable, oldValue, newValue) -> greenSpinner.getValueFactory().setValue(newValue.intValue() >> 8 & 0xff));
		greenSpinner.valueProperty().addListener((observable, oldValue, newValue) -> currentEditColor.set((currentEditColor.get() & ~0xff00) | newValue << 8));
		
		Spinner<Integer> blueSpinner = (Spinner) root.lookup("#BSpinner");
		currentEditColor.addListener((observable, oldValue, newValue) -> blueSpinner.getValueFactory().setValue(newValue.intValue() >> 16 & 0xff));
		blueSpinner.valueProperty().addListener((observable, oldValue, newValue) -> currentEditColor.set((currentEditColor.get() & ~0xff0000) | newValue << 16));
		
		Spinner<Integer> alphaSpinner = (Spinner) root.lookup("#ASpinner");
		currentEditColor.addListener((observable, oldValue, newValue) -> alphaSpinner.getValueFactory().setValue(newValue.intValue() >> 24 & 0xff));
		alphaSpinner.valueProperty().addListener((observable, oldValue, newValue) -> currentEditColor.set((currentEditColor.get() & ~0xff000000) | newValue << 24));
		
		currentEditColor.bindBidirectional(primaryColor);
		primaryColor.setValue(0xff000000);
		secondaryColor.setValue(0xffffffff);
	}
	
	public void setPrimaryColor(int value) {
		primaryColor.setValue(value);
	}
	
	public void setSecondaryColor(int value) {
		secondaryColor.setValue(value);
	}
	
	public void setCurrentEditColor(int value) {
		currentEditColor.setValue(value);
	}
	
	public int getPrimaryColor() {
		return primaryColor.get();
	}
	
	public int getSecondaryColor() {
		return secondaryColor.get();
	}
	
	public int getCurrentEditColor() {
		return currentEditColor.get();
	}
	
	public IntegerProperty primaryColorProperty() {
		return primaryColor;
	}
	
	public IntegerProperty secondaryColorProperty() {
		return secondaryColor;
	}
	
	public IntegerProperty currentEditColorProperty() {
		return currentEditColor;
	}
	
	private static ColorPicker instance;
	public static ColorPicker get() {
		return instance;
	}
}

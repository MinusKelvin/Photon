package minusk.photon.jfx;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValueBase;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;

/**
 * Created by MinusKelvin on 4/11/16.
 */
public class ColorDisplay extends Region {
	private IntegerProperty primaryColor = new SimpleIntegerProperty();
	private IntegerProperty secondaryColor = new SimpleIntegerProperty();
	private BooleanProperty primarySelected = new SimpleBooleanProperty(true);
	
	public ColorDisplay() {
		Rectangle rect = new Rectangle(20,20,34,34);
		rect.setFill(Color.WHITE);
		rect.setStroke(Color.BLACK);
		rect.setStrokeType(StrokeType.OUTSIDE);
		rect.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> primarySelected.set(false));
		rect.strokeWidthProperty().bind(new BooleanIntegerBind(primarySelected, 1, 2));
		getChildren().add(rect);
		
		rect = new Rectangle(21,21,32,32);
		rect.setFill(new ImagePattern(new Image(getClass().getResource("/minusk/photon/res/icons/background.png").toString()), 0,0,16,16,false));
		rect.setMouseTransparent(true);
		getChildren().add(rect);
		
		rect = new Rectangle(21,21,32,32);
		rect.fillProperty().bind(new GradientGenerator(secondaryColor));
		rect.setMouseTransparent(true);
		getChildren().add(rect);
		
		rect = new Rectangle(0,0,34,34);
		rect.setFill(Color.WHITE);
		rect.setStroke(Color.BLACK);
		rect.setStrokeType(StrokeType.OUTSIDE);
		rect.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> primarySelected.set(true));
		rect.strokeWidthProperty().bind(new BooleanIntegerBind(primarySelected, 2, 1));
		getChildren().add(rect);
		
		rect = new Rectangle(1,1,32,32);
		rect.setFill(new ImagePattern(new Image(getClass().getResource("/minusk/photon/res/icons/background.png").toString()), 0,0,16,16,false));
		rect.setMouseTransparent(true);
		getChildren().add(rect);
		
		rect = new Rectangle(1,1,32,32);
		rect.fillProperty().bind(new GradientGenerator(primaryColor));
		rect.setMouseTransparent(true);
		getChildren().add(rect);
	}
	
	public void setPrimaryColor(int value) {
		primaryColor.setValue(value);
	}
	
	public void setSecondaryColor(int value) {
		secondaryColor.setValue(value);
	}
	
	public void setPrimarySelected(boolean value) {
		primarySelected.set(value);
	}
	
	public int getPrimaryColor() {
		return primaryColor.get();
	}
	
	public int getSecondaryColor() {
		return secondaryColor.get();
	}
	
	public boolean getPrimarySelected() {
		return primarySelected.get();
	}
	
	public IntegerProperty primaryColorProperty() {
		return primaryColor;
	}
	
	public IntegerProperty secondaryColorProperty() {
		return secondaryColor;
	}
	
	public BooleanProperty primarySelectedProperty() {
		return primarySelected;
	}
	
	private static class GradientGenerator extends ObservableValueBase<Paint> {
		private final IntegerProperty observee;
		
		public GradientGenerator(IntegerProperty observee) {
			this.observee = observee;
			observee.addListener(observable -> fireValueChangedEvent());
		}
		
		@Override
		public Paint getValue() {
			int color = observee.get();
			Color end = new Color(extract(color,0),extract(color,8),extract(color,16),extract(color,24));
			LinearGradient gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
					new Stop(0.25, end), new Stop(1, new Color(end.getRed(),end.getGreen(),end.getBlue(),1)));
			return gradient;
		}
		
		private double extract(int color, int bits) {
			return (color >> bits & 0xff) / 255.0;
		}
	}
	
	private static class BooleanIntegerBind extends ObservableValueBase<Integer> {
		private final int on;
		private final int off;
		private final BooleanProperty observee;
		
		public BooleanIntegerBind(BooleanProperty observee, int on, int off) {
			this.observee = observee;
			this.on = on;
			this.off = off;
			observee.addListener(observable -> fireValueChangedEvent());
		}
		
		@Override
		public Integer getValue() {
			return observee.get() ? on : off;
		}
	}
}

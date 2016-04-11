package minusk.photon.jfx;

import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Region;

/**
 * Created by MinusKelvin on 4/9/16.
 */
public class Display extends Region {
	public final Canvas displayer = new Canvas();
	
	public ResizeListener resize;
	
	public Display() {
		getChildren().add(displayer);
		displayer.widthProperty().addListener(observable -> resize.invoke());
		displayer.heightProperty().addListener(observable -> resize.invoke());
	}
	
	@Override
	protected void layoutChildren() {
		super.layoutChildren();
		
		final double width = getWidth();
		final double height = getHeight();
		final Insets insets = getInsets();
		final double contentX = insets.getLeft();
		final double contentY = insets.getTop();
		final double contentWidth = Math.max(0, width - (insets.getLeft() + insets.getRight()));
		final double contentHeight = Math.max(0, height - (insets.getTop() + insets.getBottom()));
		displayer.relocate(contentX, contentY);
		displayer.setWidth(contentWidth);
		displayer.setHeight(contentHeight);
	}
	
	public interface ResizeListener {
		void invoke();
	}
}

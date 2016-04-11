package minusk.photon.tools;

import minusk.photon.control.ColorPicker;
import minusk.photon.control.MainController;
import minusk.photon.image.BlendMode;
import minusk.photon.image.Document;

import java.nio.ByteBuffer;

/**
 * Created by MinusKelvin on 1/11/15.
 */
public class Pencil implements Tool {
	private boolean inuse, color;
	private int lastx, lasty;
	
	@Override
	public void mouseMove(double dx, double dy) {
		if (inuse) {
			Document currentDocument = MainController.get().getCurrentDocument();
			
			ByteBuffer change = currentDocument.getChangeBuffer();
			ByteBuffer changeStencil = currentDocument.getChangeStencil();
			int x = (int) Math.floor(dx);
			int y = (int) Math.floor(dy);
			if (x == lastx && y == lasty)
				return;
			
			boolean slope, direction;
			double m;
			if (Math.abs(x-lastx) > Math.abs(y-lasty)) {
				slope = true;
				m = (double) (y-lasty) / (x-lastx);
				direction = x < lastx;
			} else {
				slope = false;
				m = (double) (x-lastx) / (y-lasty);
				direction = y < lasty;
			}
			
			double offset = 0;
			if (slope) {
				for (; direction ? x <= lastx : x >= lastx; x+=direction?1:-1, offset+=direction?m:-m) {
					if (offset >= 0.5) {
						offset--;
						y++;
					}
					if (offset < -0.5) {
						offset++;
						y--;
					}
					if (x >= 0 && y >= 0 && x < currentDocument.getWidth() && y < currentDocument.getHeight()) {
						change.putInt((x + y * currentDocument.getWidth()) * 4,
								color ? ColorPicker.get().getPrimaryColor() : ColorPicker.get().getSecondaryColor());
						changeStencil.putInt(x + y * currentDocument.getWidth(), 1);
						currentDocument.flush();
					}
				}
			} else {
				for (; direction ? y <= lasty : y >= lasty; y+=direction?1:-1, offset+=direction?m:-m) {
					if (offset >= 0.5) {
						offset--;
						x++;
					}
					if (offset < -0.5) {
						offset++;
						x--;
					}
					if (x >= 0 && y >= 0 && x < currentDocument.getWidth() && y < currentDocument.getHeight()) {
						change.putInt((x + y * currentDocument.getWidth()) * 4,
								color ? ColorPicker.get().getPrimaryColor() : ColorPicker.get().getSecondaryColor());
						changeStencil.putInt(x + y * currentDocument.getWidth(), 1);
						currentDocument.flush();
					}
				}
			}
			lastx = (int) Math.floor(dx);
			lasty = (int) Math.floor(dy);
		}
	}
	
	@Override
	public void mouseDown(double x, double y, boolean primary) {
		if (!inuse) {
			Document currentDocument = MainController.get().getCurrentDocument();
			currentDocument.initializeChange(BlendMode.getAlpha(), 1);
			ByteBuffer change = currentDocument.getChangeBuffer();
			ByteBuffer changeStencil = currentDocument.getChangeStencil();
			if (x >= 0 && y >= 0 && x < currentDocument.getWidth() && y < currentDocument.getHeight()) {
				change.putInt(((int) x + (int) y * currentDocument.getWidth()) * 4,
						primary ? ColorPicker.get().getPrimaryColor() : ColorPicker.get().getSecondaryColor());
				changeStencil.putInt((int) x + (int) y * currentDocument.getWidth(), 1);
				currentDocument.flush();
			}
			lastx = (int) Math.floor(x);
			lasty = (int) Math.floor(y);
			inuse = true;
			color = primary;
		}
	}
	
	@Override
	public void mouseUp(double x, double y, boolean primary) {
		if (inuse && primary == color) {
			MainController.get().getCurrentDocument().finalizeChange();
			inuse = false;
		}
	}
}

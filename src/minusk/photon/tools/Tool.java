package minusk.photon.tools;

/**
 * Created by MinusKelvin on 1/11/15.
 */
public interface Tool {
	void mouseMove(double x, double y);
	void mouseDown(double x, double y, boolean primary);
	void mouseUp(double x, double y, boolean primary);
}

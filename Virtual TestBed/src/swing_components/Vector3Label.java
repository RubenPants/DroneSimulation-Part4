package swing_components;

import javax.swing.JLabel;
import static tools.Tools.*;

import org.lwjgl.util.vector.Vector3f;

public class Vector3Label extends JLabel {
	
	public Vector3Label(Vector3f vector) {
		super();
		setText(round(vector.x,2) + " " + round(vector.y,2) + " " + round(vector.z,2));
	}
	
	public void updateValue(Vector3f vector) {
		setText(round(vector.x,2) + " " + round(vector.y,2) + " " + round(vector.z,2));
	}
}
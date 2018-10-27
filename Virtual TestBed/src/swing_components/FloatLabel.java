package swing_components;

import static tools.Tools.round;

import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class FloatLabel extends JPanel {

	private JLabel field;
	
	public FloatLabel(String value) {
		setLayout(new GridLayout());
		field = new JLabel(value);
		add(field);
	}
	
	public void updateValueToDegrees(float value) {
		field.setText(Float.toString(round(Math.toDegrees(value),2)));
	}
	
	public void updateValue(float value) {
		field.setText(Float.toString(round(value,2)));
	}
}

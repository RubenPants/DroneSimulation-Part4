package swing_components;

import static tools.Tools.round;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.lwjgl.util.vector.Vector3f;

public class InfoPanel extends JPanel {
	private FloatLabel headingLabel;
	private FloatLabel pitchLabel;
	private FloatLabel rollLabel;
	
	private FloatLabel fpsLabel;
	private FloatLabel timeLabel;
	

	private Vector3Label positionLabel;
	private Vector3Label velocityLabel;
	private Vector3Label angularVelocityLabel;
	
	public InfoPanel() {
		Dimension dim = getPreferredSize();
		dim.width = 400;
		dim.height = 200;
		setPreferredSize(dim);
		fpsLabel = new FloatLabel("50.00");
		fpsLabel.setMinimumSize(new Dimension(150, 10));
		fpsLabel.setMaximumSize(new Dimension(150, 10));
		timeLabel = new FloatLabel("0.00");
		timeLabel.setMinimumSize(new Dimension(150, 10));
		timeLabel.setMaximumSize(new Dimension(150, 10));
		
		headingLabel = new FloatLabel("0");
		headingLabel.setMinimumSize(new Dimension(150, 10));
		headingLabel.setMaximumSize(new Dimension(150, 10));
		pitchLabel = new FloatLabel("0");
		pitchLabel.setMinimumSize(new Dimension(150, 10));
		pitchLabel.setMaximumSize(new Dimension(150, 10));
		rollLabel = new FloatLabel("0");
		rollLabel.setMinimumSize(new Dimension(150, 10));
		rollLabel.setMaximumSize(new Dimension(150, 10));
		
		positionLabel = new Vector3Label(new Vector3f(0,4.7f,0));
		positionLabel.setMinimumSize(new Dimension(150, 10));
		positionLabel.setPreferredSize(new Dimension(150, 10));
		positionLabel.setMaximumSize(new Dimension(150, 10));
		velocityLabel = new Vector3Label(new Vector3f(0,0,0));
		velocityLabel.setMinimumSize(new Dimension(150, 10));
		velocityLabel.setPreferredSize(new Dimension(150, 10));
		velocityLabel.setMaximumSize(new Dimension(150, 10));
		angularVelocityLabel = new Vector3Label(new Vector3f(0,0,0));
		angularVelocityLabel.setMinimumSize(new Dimension(150, 20));
		angularVelocityLabel.setPreferredSize(new Dimension(150, 10));
		angularVelocityLabel.setMaximumSize(new Dimension(150, 10));
		
		Border innerBorder = BorderFactory.createTitledBorder("Info");
		Border outerBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
		setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
		
		setLayout(new GridBagLayout());
		
		GridBagConstraints gc = new GridBagConstraints();
		
		//////////////////////////////////////////////////////////////
		gc.gridy = 0;
		gc.gridx = 0;
		gc.ipadx = 30;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		add(new JLabel("Frames Per Second"), gc);
		
		gc.gridx = 1;
		add(fpsLabel, gc);
		//////////////////////////////////////////////////////////////
		gc.gridy++;
		gc.gridx = 0;
		add(new JLabel("Simulation Time"), gc);
		
		gc.gridx = 1;
		add(timeLabel, gc);
		//////////////////////////////////////////////////////////////
		gc.gridy++;
		gc.gridx = 0;
		add(new JLabel("Heading"), gc);
		
		gc.gridx = 1;
		add(headingLabel, gc);
		//////////////////////////////////////////////////////////////
		gc.gridy++;
		gc.gridx = 0;
		add(new JLabel("Pitch"), gc);
		
		gc.gridx = 1;
		add(pitchLabel, gc);
		//////////////////////////////////////////////////////////////
		gc.gridy++;
		gc.gridx = 0;
		add(new JLabel("Roll"), gc);
		
		gc.gridx = 1;
		add(rollLabel, gc);
		//////////////////////////////////////////////////////////////
		gc.gridy++;
		gc.gridx = 0;
		add(new JLabel("Position"), gc);
		
		gc.gridx = 1;
		add(positionLabel, gc);
		//////////////////////////////////////////////////////////////
		gc.gridy++;
		gc.gridx = 0;
		add(new JLabel("Velocity"), gc);
		
		gc.gridx = 1;
		add(velocityLabel, gc);
		//////////////////////////////////////////////////////////////
		gc.gridy++;
		gc.gridx = 0;
		add(new JLabel("Angular Velocity"), gc);
		
		gc.gridx = 1;
		add(angularVelocityLabel, gc);
		//////////////////////////////////////////////////////////////
	}
	
	public void updateOrientationLabels(float heading, float pitch, float roll) {
		headingLabel.updateValueToDegrees(heading);
		pitchLabel.updateValueToDegrees(pitch);
		rollLabel.updateValueToDegrees(roll);
	}
	 
	public void updateVelocityLabels(Vector3f position, Vector3f velocity, Vector3f angularVelocity) {
		positionLabel.updateValue(position);
		velocityLabel.updateValue(velocity);
		angularVelocityLabel.updateValue(angularVelocity);
	}
	
	public void updateTime(float frameTime, float time) {
		fpsLabel.updateValue(1/frameTime);
		timeLabel.updateValue(time);
	}
}

package swing_components;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.lwjgl.util.vector.Vector3f;

import control.WorldManager;

public class InputPanel extends JPanel {
	
	private SettingsPanel fovPanel;
	private SettingsPanel timePanel;
	
	private JCheckBox maxSpeedBox;
	
	private InfoPanel infoPanel;
	
	private CameraPanel cameraPanel;
	
	private SettingsListener settingsListener;
	
	public InputPanel() {
		Dimension dim = getPreferredSize();
		dim.width = 500;
		setPreferredSize(dim);
		
		
		ChangeListener changeListener = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				settingsListener.changeRenderSettings(new SettingsEvent(this,
								fovPanel.getValue()));
			}
		};
		
		fovPanel = new SettingsPanel("FOV: ", JSlider.HORIZONTAL, 0, 180, 120, 1);
		fovPanel.addChangeListener(changeListener);
		
		timePanel = new SettingsPanel("Time Percent (%): ", JSlider.HORIZONTAL, 20, 5000, 100, 0.01f);
		timePanel.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				settingsListener.setTime(timePanel.getValue());
			}
		});
		
		maxSpeedBox = new JCheckBox("Run MAX speed");
		maxSpeedBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				settingsListener.setMaxSpeed(maxSpeedBox.isSelected());
			}
		});
		
		infoPanel = new InfoPanel();
		
		cameraPanel = new CameraPanel();
		
		Border innerBorder = BorderFactory.createTitledBorder("Testbed Settings");
		Border outerBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
		setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
		
		setLayout(new GridBagLayout());
		
		GridBagConstraints gc = new GridBagConstraints();
		
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 1;
		gc.weighty = .8;
		gc.fill = GridBagConstraints.HORIZONTAL;
		
		add(fovPanel, gc);
		
		gc.gridy = 1;
		
		add(timePanel, gc);
		
		gc.gridy = 2;
		
		add(maxSpeedBox, gc);
		
		gc.gridy = 3;
		gc.weighty = 2.0;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		add(infoPanel, gc);
		
		gc.gridy = 4;
		gc.weighty = 2.0;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		add(cameraPanel, gc);
	}
	
	public void setSettingsListener(SettingsListener listener) {
		this.settingsListener = listener;
	}
	
	public void updateOrientationLabels(float heading, float pitch, float roll) {
		infoPanel.updateOrientationLabels(heading, pitch, roll);
	}
	 
	public void updateVelocityLabels(Vector3f position, Vector3f velocity, Vector3f angularVelocity) {
		infoPanel.updateVelocityLabels(position, velocity, angularVelocity);
	}
	
	public void updateTime(float frameTime, float time) {
		infoPanel.updateTime(frameTime, time);
	}
	
	public void fireData() {
		cameraPanel.fireData();
	}
	
	public void setWorldManager(WorldManager manager) {
		cameraPanel.setManager(manager);
	}
}

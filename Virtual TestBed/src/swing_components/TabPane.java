package swing_components;

import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.lwjgl.util.vector.Vector3f;

import control.WorldManager;
import interfaces.AutopilotConfig;

public class TabPane extends JTabbedPane{
	
	InputPanel inputPanel;
	ConfigPanel configPanel;
	PackagePanel packagePanel;
	
	public TabPane() {
		inputPanel = new InputPanel();
		configPanel = new ConfigPanel();
		packagePanel = new PackagePanel();
		
		addTab("Simulation", inputPanel);
		addTab("Configuration", configPanel);
		addTab("Packages", packagePanel);
		addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
			}
		});
		
	}

	public void setSettingsListener(SettingsListener listener) {
		inputPanel.setSettingsListener(listener);
		configPanel.setSettingsListener(listener);
	}
	
	public void updateOrientationLabels(float heading, float pitch, float roll) {
		inputPanel.updateOrientationLabels(heading, pitch, roll);
	}
	
	public void updateVelocityLabels(Vector3f position, Vector3f velocity, Vector3f angularVelocity) {
		inputPanel.updateVelocityLabels(position, velocity, angularVelocity);
	}
	
	public AutopilotConfig getConfigs() {
		return configPanel.getConfig();
	}
	
	public void updateTime(float frameTime, float time) {
		inputPanel.updateTime(frameTime, time);
	}
	
	public ConfigPanel getConfigPanel(){
		return configPanel;
	}
	
	public void fireData(WorldManager manager){
		configPanel.fireData(manager);
		packagePanel.fireData();
		inputPanel.fireData();
	}
	
	public void setWorldManager(WorldManager manager) {
		packagePanel.setWorldManager(manager);
		inputPanel.setWorldManager(manager);
	}
	
	public void refreshPackTable() {
		packagePanel.refreshTables();
	}
	
	public void addPackageToTable() {
		packagePanel.addPackageToTable();
	}
}

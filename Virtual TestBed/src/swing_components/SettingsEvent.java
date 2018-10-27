package swing_components;

import java.util.EventObject;

public class SettingsEvent extends EventObject{

	private float fov;
	
	public SettingsEvent(Object source) {
		super(source);
	}
	
	public SettingsEvent(Object source, float fov) {
		super(source);
		this.fov = fov;
	}

	public float getFov() {
		return fov;
	}
}

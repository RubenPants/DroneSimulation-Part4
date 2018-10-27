package swing_components;

public interface SettingsListener {
	public void changeRenderSettings(SettingsEvent e);
	public void setTime(float time);
	public void setDroneForCamera(int id);
	public void setMaxSpeed(boolean max);
}

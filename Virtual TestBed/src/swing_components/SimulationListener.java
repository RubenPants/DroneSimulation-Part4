package swing_components;

import control.SimulationStatus;
import rendering.CameraView;

public interface SimulationListener {
	public void swapSimulationStatus(SimulationStatus status);
	
	public void swapView(CameraView view);
}

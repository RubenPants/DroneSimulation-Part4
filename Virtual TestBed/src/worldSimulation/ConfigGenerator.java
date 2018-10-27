package worldSimulation;

import interfaces.AutopilotConfig;

public class ConfigGenerator {
	public static AutopilotConfig generate(float wingX, float wingMass, float tailZ, float tailMass,
			float engineMass, float maxThrust, float wingLift, float horStabLift, float verStabLift, float maxAOA,
			float tyreRadius, float wheelY, float frontZ, float rearZ, float rearX, float gravity){

		float totalMass = engineMass+2*wingMass+tailMass;
		float gravitationForce = totalMass*gravity;
		float tyreSlope = (gravitationForce/(3*0.1f*tyreRadius));
		
		return new AutopilotConfig() {
			public float getWingX() {
				return wingX;
			}
			public float getWingMass() {
				return wingMass;
			}
			public float getWingLiftSlope() {
				return wingLift;
			}
			public float getWheelY() {
				return wheelY;
			}
			public float getVerticalAngleOfView() {
				return 120;
			}
			public float getVerStabLiftSlope() {
				return 120;
			}
			public float getTyreSlope() {
				return tyreSlope;
			}
			public float getTyreRadius() {
				return tyreRadius;
			}
			public float getTailSize() {
				return tailZ;
			}
			public float getTailMass() {
				return tailMass;
			}
			public float getRearWheelZ() {
				return rearZ;
			}
			public float getRearWheelX() {
				return rearX;
			}
			public float getRMax() {
				return 2500;
			}
			public int getNbRows() {
				return 200;
			}
			public int getNbColumns() {
				return 200;
			}
			public float getMaxThrust() {
				return maxThrust;
			}
			public float getMaxAOA() {
				return maxAOA;
			}
			public float getHorizontalAngleOfView() {
				return 120;
			}
			public float getHorStabLiftSlope() {
				return horStabLift;
			}
			public float getGravity() {
				return gravity;
			}
			public float getFrontWheelZ() {
				return frontZ;
			}
			public float getFcMax() {
				return 2;
			}
			public float getEngineMass() {
				return engineMass;
			}
			public String getDroneID() {
				return null;
			}
			public float getDampSlope() {
				return 1000; //(float) (Math.sqrt(4*3*tyreSlope*totalMass))/3;
			}
		};
	}	
}

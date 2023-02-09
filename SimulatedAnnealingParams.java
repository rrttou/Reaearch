package edu.usc.ict.iago.agent;

public class SimulatedAnnealingParams {
	
	private double startTemprature;
	private double endTemprature;
	private double cool; //cooling degree
	private int numberOfSteps; //number of times to change
	
	 public SimulatedAnnealingParams() {
		 this.startTemprature = 1.5;
		 this.endTemprature = 0.001;
		 this.cool = 0.99;
		 this.numberOfSteps = 1;
	}
	 
	 public SimulatedAnnealingParams(double startTemp,double endTemp, double cool, int num) {
		 this.startTemprature = startTemp;
		 this.endTemprature = endTemp;
		 this.cool = cool;
		 this.numberOfSteps = num;
	}

	public double getStartTemprature() {
		return startTemprature;
	}

	public void setStartTemprature(double startTemprature) {
		this.startTemprature = startTemprature;
	}

	public double getEndTemprature() {
		return endTemprature;
	}

	public void setEndTemprature(double endTemprature) {
		this.endTemprature = endTemprature;
	}

	public double getCool() {
		return cool;
	}

	public void setCool(double cool) {
		this.cool = cool;
	}

	public int getNumberOfSteps() {
		return numberOfSteps;
	}

	public void setNumberOfSteps(int numberOfSteps) {
		this.numberOfSteps = numberOfSteps;
	}
	 

}
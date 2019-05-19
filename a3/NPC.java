package a3;

import java.util.Random;

import ray.rml.Vector3;

public class NPC {
	double locX, locY, locZ;
	boolean bigger = false;
	
	public NPC() {
		setNPCLocation();
	}
	
	public double getX() {
		return locX;
	}
	
	public double getY() {
		return locY;
	}
	
	public double getZ() {
		return locZ;
	}
	
	public boolean getBigger() {
		return bigger;
	}
	
	public void setX(double newX) {
		this.locX = newX;
	}
	
	public void setY(double newY) {
		this.locY = newY;
	}
	
	public void setZ(double newZ) {
		this.locZ = newZ;
	}
	
	public void setBigger(boolean newBigger) {
		this.bigger = newBigger;
	}
	
	public void setNPCLocation(){
		Random r = new Random();
		setX(0);
		setY(0);
		setZ(0);
	}
	
	public void updateLocation() {
		
	}
	
}

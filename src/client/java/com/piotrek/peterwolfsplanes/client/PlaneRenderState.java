package com.piotrek.peterwolfsplanes.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class PlaneRenderState extends EntityRenderState {
	public float pitch;
	public float yRot;
	public float roll;
	public float propellerAngle;
	public float speed;
	public float altitude;
	public boolean restingOnSolidGround;
}

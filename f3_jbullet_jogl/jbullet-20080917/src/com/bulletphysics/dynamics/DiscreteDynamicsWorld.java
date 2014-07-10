/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2008 Erwin Coumans  http://www.bulletphysics.com/
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from
 * the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package com.bulletphysics.dynamics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.bulletphysics.BulletGlobals;
import com.bulletphysics.BulletStats;
import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.CollisionFilterGroups;
import com.bulletphysics.collision.broadphase.Dispatcher;
import com.bulletphysics.collision.broadphase.DispatcherInfo;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.collision.dispatch.SimulationIslandManager;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.narrowphase.PersistentManifold;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.InternalTriangleIndexCallback;
import com.bulletphysics.collision.shapes.TriangleCallback;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;
import com.bulletphysics.dynamics.constraintsolver.ContactSolverInfo;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.dynamics.constraintsolver.TypedConstraint;
import com.bulletphysics.dynamics.vehicle.RaycastVehicle;
import com.bulletphysics.linearmath.CProfileManager;
import com.bulletphysics.linearmath.DebugDrawModes;
import com.bulletphysics.linearmath.IDebugDraw;
import com.bulletphysics.linearmath.MiscUtil;
import com.bulletphysics.linearmath.ScalarUtil;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.linearmath.TransformUtil;
import cz.advel.stack.Stack;
import javax.vecmath.Vector3f;

// For debug drawing only
import com.bulletphysics.collision.broadphase.BroadphaseNativeType;
import com.bulletphysics.collision.shapes.CapsuleShape;
import com.bulletphysics.collision.shapes.CompoundShape;
import com.bulletphysics.collision.shapes.ConcaveShape;
import com.bulletphysics.collision.shapes.ConvexTriangleMeshShape;
import com.bulletphysics.collision.shapes.CylinderShape;
import com.bulletphysics.collision.shapes.PolyhedralConvexShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.collision.shapes.StaticPlaneShape;
import com.bulletphysics.linearmath.VectorUtil;

/**
 * DiscreteDynamicsWorld provides discrete rigid body simulation.
 * 
 * @author jezek2
 */
public class DiscreteDynamicsWorld extends DynamicsWorld {

	protected ConstraintSolver constraintSolver;
	protected SimulationIslandManager islandManager;
	protected final List<TypedConstraint> constraints = new ArrayList<TypedConstraint>();
	protected final Vector3f gravity = new Vector3f(0f, -10f, 0f);

	//for variable timesteps
	protected float localTime = 1f / 60f;
	//for variable timesteps

	protected boolean ownsIslandManager;
	protected boolean ownsConstraintSolver;

	protected List<RaycastVehicle> vehicles = new ArrayList<RaycastVehicle>();
	protected int profileTimings = 0;
	
	public DiscreteDynamicsWorld(Dispatcher dispatcher, BroadphaseInterface pairCache, ConstraintSolver constraintSolver, CollisionConfiguration collisionConfiguration) {
		super(dispatcher, pairCache, collisionConfiguration);
		this.constraintSolver = constraintSolver;

		if (this.constraintSolver == null) {
			this.constraintSolver = new SequentialImpulseConstraintSolver();
			ownsConstraintSolver = true;
		}
		else {
			ownsConstraintSolver = false;
		}

		{
			islandManager = new SimulationIslandManager();
		}

		ownsIslandManager = true;
	}

	protected void saveKinematicState(float timeStep) {
		for (int i = 0; i < collisionObjects.size(); i++) {
			CollisionObject colObj = collisionObjects.get(i);
			RigidBody body = RigidBody.upcast(colObj);
			if (body != null) {
				//Transform predictedTrans = new Transform();
				if (body.getActivationState() != CollisionObject.ISLAND_SLEEPING) {
					if (body.isKinematicObject()) {
                                            // to calculate velocities next frame
                                            body.saveKinematicState(timeStep);
					}
				}
			}
		}
	}

	@Override
	public void debugDrawWorld() {
		if (getDebugDrawer() != null && (getDebugDrawer().getDebugMode() & DebugDrawModes.DRAW_CONTACT_POINTS) != 0) {
			int numManifolds = getDispatcher().getNumManifolds();
			Vector3f color = Stack.alloc(Vector3f.class);
			color.set(0f, 0f, 0f);
			for (int i = 0; i < numManifolds; i++) {
				PersistentManifold contactManifold = getDispatcher().getManifoldByIndexInternal(i);
				//btCollisionObject* obA = static_cast<btCollisionObject*>(contactManifold->getBody0());
				//btCollisionObject* obB = static_cast<btCollisionObject*>(contactManifold->getBody1());

				int numContacts = contactManifold.getNumContacts();
				for (int j = 0; j < numContacts; j++) {
					ManifoldPoint cp = contactManifold.getContactPoint(j);
					getDebugDrawer().drawContactPoint(cp.positionWorldOnB, cp.normalWorldOnB, cp.getDistance(), cp.getLifeTime(), color);
				}
			}
		}
		
		if (getDebugDrawer() != null && (getDebugDrawer().getDebugMode() & (DebugDrawModes.DRAW_WIREFRAME | DebugDrawModes.DRAW_AABB)) != 0) {
			int i;

			Transform tmpTrans = Stack.alloc(Transform.class);
			Vector3f minAabb = Stack.alloc(Vector3f.class);
			Vector3f maxAabb = Stack.alloc(Vector3f.class);
			Vector3f colorvec = Stack.alloc(Vector3f.class);
			
			// todo: iterate over awake simulation islands!
			for (i = 0; i < collisionObjects.size(); i++) {
				CollisionObject colObj = collisionObjects.get(i);
				if (getDebugDrawer() != null && (getDebugDrawer().getDebugMode() & DebugDrawModes.DRAW_WIREFRAME) != 0) {
					Vector3f color = Stack.alloc(Vector3f.class);
					color.set(255f, 255f, 255f);
					switch (colObj.getActivationState()) {
						case CollisionObject.ACTIVE_TAG:
							color.set(255f, 255f, 255f);
							break;
						case CollisionObject.ISLAND_SLEEPING:
							color.set(0f, 255f, 0f);
							break;
						case CollisionObject.WANTS_DEACTIVATION:
							color.set(0f, 255f, 255f);
							break;
						case CollisionObject.DISABLE_DEACTIVATION:
							color.set(255f, 0f, 0f);
							break;
						case CollisionObject.DISABLE_SIMULATION:
							color.set(255f, 255f, 0f);
							break;
						default: {
							color.set(255f, 0f, 0f);
						}
					}

					debugDrawObject(colObj.getWorldTransform(tmpTrans), colObj.getCollisionShape(), color);
				}
				if (debugDrawer != null && (debugDrawer.getDebugMode() & DebugDrawModes.DRAW_AABB) != 0) {
					colorvec.set(1f, 0f, 0f);
					colObj.getCollisionShape().getAabb(colObj.getWorldTransform(tmpTrans), minAabb, maxAabb);
					debugDrawer.drawAabb(minAabb, maxAabb, colorvec);
				}
			}

			Vector3f wheelColor = Stack.alloc(Vector3f.class);
			Vector3f wheelPosWS = Stack.alloc(Vector3f.class);
			Vector3f axle = Stack.alloc(Vector3f.class);
			Vector3f tmp = Stack.alloc(Vector3f.class);

			for (i = 0; i < vehicles.size(); i++) {
				for (int v = 0; v < vehicles.get(i).getNumWheels(); v++) {
					wheelColor.set(0, 255, 255);
					if (vehicles.get(i).getWheelInfo(v).raycastInfo.isInContact) {
						wheelColor.set(0, 0, 255);
					}
					else {
						wheelColor.set(255, 0, 255);
					}

					wheelPosWS.set(vehicles.get(i).getWheelInfo(v).worldTransform.origin);

					axle.set(
							vehicles.get(i).getWheelInfo(v).worldTransform.basis.getElement(0, vehicles.get(i).getRightAxis()),
							vehicles.get(i).getWheelInfo(v).worldTransform.basis.getElement(1, vehicles.get(i).getRightAxis()),
							vehicles.get(i).getWheelInfo(v).worldTransform.basis.getElement(2, vehicles.get(i).getRightAxis()));


					//m_vehicles[i]->getWheelInfo(v).m_raycastInfo.m_wheelAxleWS
					//debug wheels (cylinders)
					tmp.add(wheelPosWS, axle);
					debugDrawer.drawLine(wheelPosWS, tmp, wheelColor);
					debugDrawer.drawLine(wheelPosWS, vehicles.get(i).getWheelInfo(v).raycastInfo.contactPointWS, wheelColor);
				}
			}
		}
	}

	@Override
	public void clearForces() {
		// todo: iterate over awake simulation islands!
		for (int i = 0; i < collisionObjects.size(); i++) {
			CollisionObject colObj = collisionObjects.get(i);

			RigidBody body = RigidBody.upcast(colObj);
			if (body != null) {
				body.clearForces();
			}
		}
	}
	
	/**
	 * Apply gravity, call this once per timestep.
	 */
	public void applyGravity() {
		// todo: iterate over awake simulation islands!
		for (int i = 0; i < collisionObjects.size(); i++) {
			CollisionObject colObj = collisionObjects.get(i);

			RigidBody body = RigidBody.upcast(colObj);
			if (body != null && body.isActive()) {
				body.applyGravity();
			}
		}
	}

	protected void synchronizeMotionStates() {
		Transform interpolatedTransform = Stack.alloc(Transform.class);
		
		Transform tmpTrans = Stack.alloc(Transform.class);
		Vector3f tmpLinVel = Stack.alloc(Vector3f.class);
		Vector3f tmpAngVel = Stack.alloc(Vector3f.class);

		// todo: iterate over awake simulation islands!
		for (int i = 0; i < collisionObjects.size(); i++) {
			CollisionObject colObj = collisionObjects.get(i);

			RigidBody body = RigidBody.upcast(colObj);
			if (body != null && body.getMotionState() != null && !body.isStaticOrKinematicObject()) {
				// we need to call the update at least once, even for sleeping objects
				// otherwise the 'graphics' transform never updates properly
				// so todo: add 'dirty' flag
				//if (body->getActivationState() != ISLAND_SLEEPING)
				{
					TransformUtil.integrateTransform(
							body.getInterpolationWorldTransform(tmpTrans),
							body.getInterpolationLinearVelocity(tmpLinVel),
							body.getInterpolationAngularVelocity(tmpAngVel),
							localTime, interpolatedTransform);
                                        if (!body.isStaticOrKinematicObject()) {
                                            body.getMotionState().setWorldTransform(interpolatedTransform);
                                        }
				}
			}
		}

		if (getDebugDrawer() != null && (getDebugDrawer().getDebugMode() & DebugDrawModes.DRAW_WIREFRAME) != 0) {
			for (int i = 0; i < vehicles.size(); i++) {
				for (int v = 0; v < vehicles.get(i).getNumWheels(); v++) {
					// synchronize the wheels with the (interpolated) chassis worldtransform
					vehicles.get(i).updateWheelTransform(v, true);
				}
			}
		}
	}

	@Override
	public int stepSimulation(float timeStep, int maxSubSteps, float fixedTimeStep) {
		startProfiling(timeStep);

		long t0 = System.nanoTime();
		
		BulletStats.pushProfile("stepSimulation");
		try {
			int numSimulationSubSteps = 0;

			if (maxSubSteps != 0) {
				// fixed timestep with interpolation
				localTime += timeStep;
				if (localTime >= fixedTimeStep) {
					numSimulationSubSteps = (int) (localTime / fixedTimeStep);
					localTime -= numSimulationSubSteps * fixedTimeStep;
				}
			}
			else {
				//variable timestep
				fixedTimeStep = timeStep;
				localTime = timeStep;
				if (ScalarUtil.fuzzyZero(timeStep)) {
					numSimulationSubSteps = 0;
					maxSubSteps = 0;
				}
				else {
					numSimulationSubSteps = 1;
					maxSubSteps = 1;
				}
			}

			// process some debugging flags
			if (getDebugDrawer() != null) {
				BulletGlobals.setDeactivationDisabled((getDebugDrawer().getDebugMode() & DebugDrawModes.NO_DEACTIVATION) != 0);
			}
                        
			if (numSimulationSubSteps != 0) {
                                saveKinematicState(fixedTimeStep);

				applyGravity();

				// clamp the number of substeps, to prevent simulation grinding spiralling down to a halt
				int clampedSimulationSteps = (numSimulationSubSteps > maxSubSteps) ? maxSubSteps : numSimulationSubSteps;

				for (int i = 0; i < clampedSimulationSteps; i++) {
                                    internalSingleStepSimulation(fixedTimeStep);
                                    synchronizeMotionStates();
				}
			}

			synchronizeMotionStates();

			clearForces();

			//#ifndef BT_NO_PROFILE
			CProfileManager.incrementFrameCounter();
			//#endif //BT_NO_PROFILE

			return numSimulationSubSteps;
		}
		finally {
			BulletStats.popProfile();
			
			BulletStats.stepSimulationTime = (System.nanoTime() - t0) / 1000000;
		}
	}

	protected void internalSingleStepSimulation(float timeStep) {
		BulletStats.pushProfile("internalSingleStepSimulation");
		try {
			// apply gravity, predict motion
			predictUnconstraintMotion(timeStep);

			DispatcherInfo dispatchInfo = getDispatchInfo();

			dispatchInfo.timeStep = timeStep;
			dispatchInfo.stepCount = 0;
			dispatchInfo.debugDraw = getDebugDrawer();

			// perform collision detection
			performDiscreteCollisionDetection();

			calculateSimulationIslands();

			getSolverInfo().timeStep = timeStep;

			// solve contact and other joint constraints
			solveConstraints(getSolverInfo());

			//CallbackTriggers();

			// integrate transforms
			integrateTransforms(timeStep);

			// update vehicle simulation
			updateVehicles(timeStep);

			updateActivationState(timeStep);
			
			if (internalTickCallback != null) {
				internalTickCallback.internalTick(this, timeStep);
			}
		}
		finally {
			BulletStats.popProfile();
		}
	}

	@Override
	public void setGravity(Vector3f gravity) {
		this.gravity.set(gravity);
		for (int i = 0; i < collisionObjects.size(); i++) {
			CollisionObject colObj = collisionObjects.get(i);
			RigidBody body = RigidBody.upcast(colObj);
			if (body != null) {
				body.setGravity(gravity);
			}
		}
	}
	
	@Override
	public Vector3f getGravity(Vector3f out) {
		out.set(gravity);
		return out;
	}

	@Override
	public void removeRigidBody(RigidBody body) {
		removeCollisionObject(body);
	}

	@Override
	public void addRigidBody(RigidBody body) {
		if (!body.isStaticOrKinematicObject()) {
			body.setGravity(gravity);
		}

		if (body.getCollisionShape() != null) {
			boolean isDynamic = !(body.isStaticObject() || body.isKinematicObject());
			short collisionFilterGroup = isDynamic ? (short) CollisionFilterGroups.DEFAULT_FILTER : (short) CollisionFilterGroups.STATIC_FILTER;
			short collisionFilterMask = isDynamic ? (short) CollisionFilterGroups.ALL_FILTER : (short) (CollisionFilterGroups.ALL_FILTER ^ CollisionFilterGroups.STATIC_FILTER);

			addCollisionObject(body, collisionFilterGroup, collisionFilterMask);
		}
	}

	public void addRigidBody(RigidBody body, short group, short mask) {
		if (!body.isStaticOrKinematicObject()) {
			body.setGravity(gravity);
		}

		if (body.getCollisionShape() != null) {
			addCollisionObject(body, group, mask);
		}
	}

	protected void updateVehicles(float timeStep) {
		BulletStats.pushProfile("updateVehicles");
		try {
			for (int i = 0; i < vehicles.size(); i++) {
				RaycastVehicle vehicle = vehicles.get(i);
				vehicle.updateVehicle(timeStep);
			}
		}
		finally {
			BulletStats.popProfile();
		}
	}

	protected void updateActivationState(float timeStep) {
		BulletStats.pushProfile("updateActivationState");
		try {
			for (int i = 0; i < collisionObjects.size(); i++) {
				CollisionObject colObj = collisionObjects.get(i);
				RigidBody body = RigidBody.upcast(colObj);
				if (body != null) {
					body.updateDeactivation(timeStep);

					if (body.wantsSleeping()) {
						if (body.isStaticOrKinematicObject()) {
							body.setActivationState(CollisionObject.ISLAND_SLEEPING);
						}
						else {
							if (body.getActivationState() == CollisionObject.ACTIVE_TAG) {
								body.setActivationState(CollisionObject.WANTS_DEACTIVATION);
							}
						}
					}
					else {
						if (body.getActivationState() != CollisionObject.DISABLE_DEACTIVATION) {
							body.setActivationState(CollisionObject.ACTIVE_TAG);
						}
					}
				}
			}
		}
		finally {
			BulletStats.popProfile();
		}
	}

	@Override
	public void addConstraint(TypedConstraint constraint, boolean disableCollisionsBetweenLinkedBodies) {
		constraints.add(constraint);
		if (disableCollisionsBetweenLinkedBodies) {
			constraint.getRigidBodyA().addConstraintRef(constraint);
			constraint.getRigidBodyB().addConstraintRef(constraint);
		}
	}

	@Override
	public void removeConstraint(TypedConstraint constraint) {
		constraints.remove(constraint);
		constraint.getRigidBodyA().removeConstraintRef(constraint);
		constraint.getRigidBodyB().removeConstraintRef(constraint);
	}
	
	@Override
	public void addVehicle(RaycastVehicle vehicle) {
		vehicles.add(vehicle);
	}
	
	@Override
	public void removeVehicle(RaycastVehicle vehicle) {
		vehicles.remove(vehicle);
	}
	
	private static int getConstraintIslandId(TypedConstraint lhs) {
		int islandId;

		CollisionObject rcolObj0 = lhs.getRigidBodyA();
		CollisionObject rcolObj1 = lhs.getRigidBodyB();
		islandId = rcolObj0.getIslandTag() >= 0 ? rcolObj0.getIslandTag() : rcolObj1.getIslandTag();
		return islandId;
	}
	
	private static class InplaceSolverIslandCallback implements SimulationIslandManager.IslandCallback {
		public ContactSolverInfo solverInfo;
		public ConstraintSolver solver;
		public List<TypedConstraint> sortedConstraints;
		public int numConstraints;
		public IDebugDraw debugDrawer;
		//public StackAlloc* m_stackAlloc;
		public Dispatcher dispatcher;

		public void init(ContactSolverInfo solverInfo, ConstraintSolver solver, List<TypedConstraint> sortedConstraints, int numConstraints, IDebugDraw debugDrawer, Dispatcher dispatcher) {
			this.solverInfo = solverInfo;
			this.solver = solver;
			this.sortedConstraints = sortedConstraints;
			this.numConstraints = numConstraints;
			this.debugDrawer = debugDrawer;
			this.dispatcher = dispatcher;
		}

		public void processIsland(List<CollisionObject> bodies, int numBodies, List<PersistentManifold> manifolds, int manifolds_offset, int numManifolds, int islandId) {
			if (islandId < 0) {
				// we don't split islands, so all constraints/contact manifolds/bodies are passed into the solver regardless the island id
				solver.solveGroup(bodies, numBodies, manifolds, manifolds_offset, numManifolds, sortedConstraints, 0, numConstraints, solverInfo, debugDrawer/*,m_stackAlloc*/, dispatcher);
			}
			else {
				// also add all non-contact constraints/joints for this island
				//List<TypedConstraint> startConstraint = null;
				int startConstraint_idx = -1;
				int numCurConstraints = 0;
				int i;

				// find the first constraint for this island
				for (i = 0; i < numConstraints; i++) {
					if (getConstraintIslandId(sortedConstraints.get(i)) == islandId) {
						//startConstraint = &m_sortedConstraints[i];
						//startConstraint = sortedConstraints.subList(i, sortedConstraints.size());
						startConstraint_idx = i;
						break;
					}
				}
				// count the number of constraints in this island
				for (; i < numConstraints; i++) {
					if (getConstraintIslandId(sortedConstraints.get(i)) == islandId) {
						numCurConstraints++;
					}
				}

				// only call solveGroup if there is some work: avoid virtual function call, its overhead can be excessive
				if ((numManifolds + numCurConstraints) > 0) {
					solver.solveGroup(bodies, numBodies, manifolds, manifolds_offset, numManifolds, sortedConstraints, startConstraint_idx, numCurConstraints, solverInfo, debugDrawer/*,m_stackAlloc*/, dispatcher);
				}
			}
		}
	}

	private List<TypedConstraint> sortedConstraints = new ArrayList<TypedConstraint>();
	private InplaceSolverIslandCallback solverCallback = new InplaceSolverIslandCallback();
	
	protected void solveConstraints(ContactSolverInfo solverInfo) {
		BulletStats.pushProfile("solveConstraints");
		try {
			// sorted version of all btTypedConstraint, based on islandId
			sortedConstraints.clear();
			for (int i=0; i<constraints.size(); i++) {
				sortedConstraints.add(constraints.get(i));
			}
			//Collections.sort(sortedConstraints, sortConstraintOnIslandPredicate);
			MiscUtil.quickSort(sortedConstraints, sortConstraintOnIslandPredicate);

			List<TypedConstraint> constraintsPtr = getNumConstraints() != 0 ? sortedConstraints : null;

			solverCallback.init(solverInfo, constraintSolver, constraintsPtr, sortedConstraints.size(), debugDrawer/*,m_stackAlloc*/, dispatcher1);

			constraintSolver.prepareSolve(getCollisionWorld().getNumCollisionObjects(), getCollisionWorld().getDispatcher().getNumManifolds());

			// solve all the constraints for this island
			islandManager.buildAndProcessIslands(getCollisionWorld().getDispatcher(), getCollisionWorld().getCollisionObjectArray(), solverCallback);

			constraintSolver.allSolved(solverInfo, debugDrawer/*, m_stackAlloc*/);
		}
		finally {
			BulletStats.popProfile();
		}
	}

	protected void calculateSimulationIslands() {
		BulletStats.pushProfile("calculateSimulationIslands");
		try {
			getSimulationIslandManager().updateActivationState(getCollisionWorld(), getCollisionWorld().getDispatcher());

			{
				int i;
				int numConstraints = constraints.size();
				for (i = 0; i < numConstraints; i++) {
					TypedConstraint constraint = constraints.get(i);

					RigidBody colObj0 = constraint.getRigidBodyA();
					RigidBody colObj1 = constraint.getRigidBodyB();

					if (((colObj0 != null) && ((colObj0).mergesSimulationIslands())) &&
							((colObj1 != null) && ((colObj1).mergesSimulationIslands()))) {
						if (colObj0.isActive() || colObj1.isActive()) {

							getSimulationIslandManager().getUnionFind().unite((colObj0).getIslandTag(),
									(colObj1).getIslandTag());
						}
					}
				}
			}

			// Store the island id in each body
			getSimulationIslandManager().storeIslandActivationState(getCollisionWorld());
		}
		finally {
			BulletStats.popProfile();
		}
	}

	protected void integrateTransforms(float timeStep) {
		BulletStats.pushProfile("integrateTransforms");
		try {
			Transform predictedTrans = Stack.alloc(Transform.class);
			for (int i = 0; i < collisionObjects.size(); i++) {
				CollisionObject colObj = collisionObjects.get(i);
				RigidBody body = RigidBody.upcast(colObj);
				if (body != null) {
					if (body.isActive() && (!body.isStaticOrKinematicObject())) {
						body.predictIntegratedTransform(timeStep, predictedTrans);
						body.proceedToTransform(predictedTrans);
					}
				}
			}
		}
		finally {
			BulletStats.popProfile();
		}
	}
	
	protected void predictUnconstraintMotion(float timeStep) {
		BulletStats.pushProfile("predictUnconstraintMotion");
		try {
			Transform tmpTrans = Stack.alloc(Transform.class);
			
			for (int i = 0; i < collisionObjects.size(); i++) {
				CollisionObject colObj = collisionObjects.get(i);
				RigidBody body = RigidBody.upcast(colObj);
				if (body != null) {
					if (!body.isStaticOrKinematicObject()) {
						if (body.isActive()) {
							body.integrateVelocities(timeStep);
							// damping
							body.applyDamping(timeStep);

							body.predictIntegratedTransform(timeStep, body.getInterpolationWorldTransform(tmpTrans));
						}
					}
				}
			}
		}
		finally {
			BulletStats.popProfile();
		}
	}
	
	protected void startProfiling(float timeStep) {
		//#ifndef BT_NO_PROFILE
        CProfileManager.reset();
		//#endif //BT_NO_PROFILE
	}

	protected void debugDrawSphere(float radius, Transform transform, Vector3f color) {
		Vector3f start = Stack.alloc(transform.origin);

		Vector3f xoffs = Stack.alloc(Vector3f.class);
		xoffs.set(radius, 0, 0);
		transform.basis.transform(xoffs);
		Vector3f yoffs = Stack.alloc(Vector3f.class);
		yoffs.set(0, radius, 0);
		transform.basis.transform(yoffs);
		Vector3f zoffs = Stack.alloc(Vector3f.class);
		zoffs.set(0, 0, radius);
		transform.basis.transform(zoffs);

		Vector3f tmp1 = Stack.alloc(Vector3f.class);
		Vector3f tmp2 = Stack.alloc(Vector3f.class);

		// XY
		tmp1.sub(start, xoffs);
		tmp2.add(start, yoffs);
		getDebugDrawer().drawLine(tmp1, tmp2, color);
		tmp1.add(start, yoffs);
		tmp2.add(start, xoffs);
		getDebugDrawer().drawLine(tmp1, tmp2, color);
		tmp1.add(start, xoffs);
		tmp2.sub(start, yoffs);
		getDebugDrawer().drawLine(tmp1, tmp2, color);
		tmp1.sub(start, yoffs);
		tmp2.sub(start, xoffs);
		getDebugDrawer().drawLine(tmp1, tmp2, color);

		// XZ
		tmp1.sub(start, xoffs);
		tmp2.add(start, zoffs);
		getDebugDrawer().drawLine(tmp1, tmp2, color);
		tmp1.add(start, zoffs);
		tmp2.add(start, xoffs);
		getDebugDrawer().drawLine(tmp1, tmp2, color);
		tmp1.add(start, xoffs);
		tmp2.sub(start, zoffs);
		getDebugDrawer().drawLine(tmp1, tmp2, color);
		tmp1.sub(start, zoffs);
		tmp2.sub(start, xoffs);
		getDebugDrawer().drawLine(tmp1, tmp2, color);

		// YZ
		tmp1.sub(start, yoffs);
		tmp2.add(start, zoffs);
		getDebugDrawer().drawLine(tmp1, tmp2, color);
		tmp1.add(start, zoffs);
		tmp2.add(start, yoffs);
		getDebugDrawer().drawLine(tmp1, tmp2, color);
		tmp1.add(start, yoffs);
		tmp2.sub(start, zoffs);
		getDebugDrawer().drawLine(tmp1, tmp2, color);
		tmp1.sub(start, zoffs);
		tmp2.sub(start, yoffs);
		getDebugDrawer().drawLine(tmp1, tmp2, color);
	}
	
	public void debugDrawObject(Transform worldTransform, CollisionShape shape, Vector3f color) {
		Vector3f tmp = Stack.alloc(Vector3f.class);
		Vector3f tmp2 = Stack.alloc(Vector3f.class);

		// Draw a small simplex at the center of the object
		{
			Vector3f start = Stack.alloc(worldTransform.origin);

			tmp.set(1f, 0f, 0f);
			worldTransform.basis.transform(tmp);
			tmp.add(start);
			tmp2.set(1f, 0f, 0f);
			getDebugDrawer().drawLine(start, tmp, tmp2);

			tmp.set(0f, 1f, 0f);
			worldTransform.basis.transform(tmp);
			tmp.add(start);
			tmp2.set(0f, 1f, 0f);
			getDebugDrawer().drawLine(start, tmp, tmp2);

			tmp.set(0f, 0f, 1f);
			worldTransform.basis.transform(tmp);
			tmp.add(start);
			tmp2.set(0f, 0f, 1f);
			getDebugDrawer().drawLine(start, tmp, tmp2);
		}

		if (shape.getShapeType() == BroadphaseNativeType.COMPOUND_SHAPE_PROXYTYPE) {
                    CompoundShape compoundShape = (CompoundShape) shape;
                    Transform childTrans = new Transform();
                    for (int i=compoundShape.getNumChildShapes()-1;i>=0;i--) {
                        compoundShape.getChildTransform(i, childTrans);
                        CollisionShape colShape = compoundShape.getChildShape(i);
                        Transform tmpTrans = new Transform();
                        tmpTrans.mul(worldTransform, childTrans);
                        debugDrawObject(tmpTrans, colShape, color);
                    }
                } else {
                    switch (shape.getShapeType()) {
                        case SPHERE_SHAPE_PROXYTYPE: {
                            SphereShape sphereShape = (SphereShape) shape;
                            float radius = sphereShape.getMargin();//radius doesn't include the margin, so draw with margin

                            debugDrawSphere(radius, worldTransform, color);
                            break;
                        }

// FIXME: no MultiSphereShape in JBullet
//			case MULTI_SPHERE_SHAPE_PROXYTYPE:
//				{
//					const btMultiSphereShape* multiSphereShape = static_cast<const btMultiSphereShape*>(shape);
//
//					for (int i = multiSphereShape->getSphereCount()-1; i>=0;i--)
//					{
//						btTransform childTransform = worldTransform;
//						childTransform.getOrigin() += multiSphereShape->getSpherePosition(i);
//						debugDrawSphere(multiSphereShape->getSphereRadius(i), childTransform, color);
//					}
//
//					break;
//				}

                        case CAPSULE_SHAPE_PROXYTYPE: {
                            CapsuleShape capsuleShape = (CapsuleShape) shape;

                            float radius = capsuleShape.getRadius();
                            float halfHeight = capsuleShape.getHalfHeight();
					
                            int upAxis = capsuleShape.getUpAxis();
					
                            Vector3f capStart = new Vector3f();
                            VectorUtil.setCoord(capStart, upAxis, -halfHeight);
                            Vector3f capEnd = new Vector3f();
                            VectorUtil.setCoord(capEnd, upAxis, halfHeight);

                            // Draw the ends
                            {
                                Transform childTransform = new Transform(worldTransform);
                                childTransform.origin.set(capStart);
                                worldTransform.transform(childTransform.origin);
                                debugDrawSphere(radius, childTransform, color);
                            }

                            {
                                Transform childTransform = new Transform(worldTransform);
                                childTransform.origin.set(capEnd);
                                worldTransform.transform(childTransform.origin);
                                debugDrawSphere(radius, childTransform, color);
                            }

                            // Draw some additional lines
                            Vector3f start = worldTransform.origin;
                            VectorUtil.setCoord(capStart, (upAxis+1)%3, radius);
                            VectorUtil.setCoord(capEnd, (upAxis+1)%3, radius);
                            getDebugDrawer().drawLine(VectorUtil.plus(start, VectorUtil.times(worldTransform.basis, capStart)),
                                                      VectorUtil.plus(start, VectorUtil.times(worldTransform.basis, capEnd)),
                                                      color);
                            VectorUtil.setCoord(capStart, (upAxis+1)%3, -radius);
                            VectorUtil.setCoord(capEnd, (upAxis+1)%3, -radius);
                            getDebugDrawer().drawLine(VectorUtil.plus(start, VectorUtil.times(worldTransform.basis, capStart)),
                                                      VectorUtil.plus(start, VectorUtil.times(worldTransform.basis, capEnd)),
                                                      color);

                            VectorUtil.setCoord(capStart, (upAxis+1)%3, 0.f);
                            VectorUtil.setCoord(capEnd, (upAxis+1)%3, 0.f);

                            VectorUtil.setCoord(capStart, (upAxis+2)%3, radius);
                            VectorUtil.setCoord(capEnd, (upAxis+2)%3, radius);
                            getDebugDrawer().drawLine(VectorUtil.plus(start, VectorUtil.times(worldTransform.basis, capStart)),
                                                      VectorUtil.plus(start, VectorUtil.times(worldTransform.basis, capEnd)),
                                                      color);
                            VectorUtil.setCoord(capStart, (upAxis+2)%3, -radius);
                            VectorUtil.setCoord(capEnd, (upAxis+2)%3, -radius);
                            getDebugDrawer().drawLine(VectorUtil.plus(start, VectorUtil.times(worldTransform.basis, capStart)),
                                                      VectorUtil.plus(start, VectorUtil.times(worldTransform.basis, capEnd)),
                                                      color);
					
                            break;
                        }
                            
// FIXME: no ConeShape in JBullet
//			case CONE_SHAPE_PROXYTYPE:
//				{
//					const btConeShape* coneShape = static_cast<const btConeShape*>(shape);
//					btScalar radius = coneShape->getRadius();//+coneShape->getMargin();
//					btScalar height = coneShape->getHeight();//+coneShape->getMargin();
//					btVector3 start = worldTransform.getOrigin();
//
//					int upAxis= coneShape->getConeUpIndex();
//					
//
//					btVector3	offsetHeight(0,0,0);
//					offsetHeight[upAxis] = height * btScalar(0.5);
//					btVector3	offsetRadius(0,0,0);
//					offsetRadius[(upAxis+1)%3] = radius;
//					btVector3	offset2Radius(0,0,0);
//					offset2Radius[(upAxis+2)%3] = radius;
//
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * (offsetHeight),start+worldTransform.getBasis() * (-offsetHeight+offsetRadius),color);
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * (offsetHeight),start+worldTransform.getBasis() * (-offsetHeight-offsetRadius),color);
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * (offsetHeight),start+worldTransform.getBasis() * (-offsetHeight+offset2Radius),color);
//					getDebugDrawer()->drawLine(start+worldTransform.getBasis() * (offsetHeight),start+worldTransform.getBasis() * (-offsetHeight-offset2Radius),color);
//
//
//
//					break;
//
//				}

                        case CYLINDER_SHAPE_PROXYTYPE: {
                            CylinderShape cylinder = (CylinderShape) shape;
                            int upAxis = cylinder.getUpAxis();
                            float radius = cylinder.getRadius();
                            Vector3f tmp1 = new Vector3f();
                            float halfHeight = VectorUtil.getCoord(cylinder.getHalfExtentsWithMargin(tmp), upAxis);
                            Vector3f start = worldTransform.origin;
                            Vector3f offsetHeight = new Vector3f();
                            VectorUtil.setCoord(offsetHeight, upAxis, halfHeight);
                            Vector3f offsetRadius = new Vector3f();
                            VectorUtil.setCoord(offsetRadius, (upAxis+1)%3, radius);
                            getDebugDrawer().drawLine(VectorUtil.plus(start, VectorUtil.times(worldTransform.basis, VectorUtil.plus(offsetHeight, offsetRadius))),
                                                      VectorUtil.plus(start, VectorUtil.times(worldTransform.basis, VectorUtil.plus(VectorUtil.times(offsetHeight, -1), offsetRadius))),
                                                      color);
                            getDebugDrawer().drawLine(VectorUtil.plus(start, VectorUtil.times(worldTransform.basis, VectorUtil.minus(offsetHeight, offsetRadius))),
                                                      VectorUtil.plus(start, VectorUtil.times(worldTransform.basis, VectorUtil.minus(VectorUtil.times(offsetHeight, -1), offsetRadius))),
                                                      color);
                            break;
                        }

                        case STATIC_PLANE_PROXYTYPE: {
                            StaticPlaneShape staticPlaneShape = (StaticPlaneShape) shape;
                            float planeConst = staticPlaneShape.getPlaneConstant();
                            Vector3f planeNormal = new Vector3f();
                            staticPlaneShape.getPlaneNormal(planeNormal);
                            Vector3f planeOrigin = VectorUtil.times(planeNormal, planeConst);
                            Vector3f vec0 = new Vector3f();
                            Vector3f vec1 = new Vector3f();
                            TransformUtil.planeSpace1(planeNormal, vec0, vec1);
                            float vecLen = 100.f;
                            Vector3f pt0 = VectorUtil.plus(planeOrigin, VectorUtil.times(vec0, vecLen));
                            Vector3f pt1 = VectorUtil.minus(planeOrigin, VectorUtil.times(vec0, vecLen));
                            Vector3f pt2 = VectorUtil.plus(planeOrigin, VectorUtil.times(vec1, vecLen));
                            Vector3f pt3 = VectorUtil.minus(planeOrigin, VectorUtil.times(vec1, vecLen));
                            getDebugDrawer().drawLine(VectorUtil.times(worldTransform, pt0), VectorUtil.times(worldTransform, pt1),color);
                            getDebugDrawer().drawLine(VectorUtil.times(worldTransform, pt2), VectorUtil.times(worldTransform, pt3),color);
                            break;
                        }

                        default: {

                            if (shape.isConcave()) {
                                ConcaveShape concaveMesh = (ConcaveShape) shape;
						
                                ///@todo pass camera, for some culling? no -> we are not a graphics lib
                                Vector3f aabbMax = new Vector3f(1e30f, 1e30f, 1e30f);
                                Vector3f aabbMin = new Vector3f(-1e30f, -1e30f, -1e30f);

                                DebugDrawcallback drawCallback = new DebugDrawcallback(getDebugDrawer(), worldTransform, color);
                                concaveMesh.processAllTriangles(drawCallback, aabbMin, aabbMax);
                            }

                            if (shape.getShapeType() == BroadphaseNativeType.CONVEX_TRIANGLEMESH_SHAPE_PROXYTYPE) {
                                ConvexTriangleMeshShape convexMesh = (ConvexTriangleMeshShape) shape;
                                //todo: pass camera for some culling			
                                Vector3f aabbMax = new Vector3f(1e30f, 1e30f, 1e30f);
                                Vector3f aabbMin = new Vector3f(-1e30f, -1e30f, -1e30f);
                                DebugDrawcallback drawCallback = new DebugDrawcallback(getDebugDrawer(), worldTransform, color);
                                convexMesh.getMeshInterface().internalProcessAllTriangles(drawCallback, aabbMin, aabbMax);
                            }

                            /// for polyhedral shapes
                            if (shape.isPolyhedral()) {
                                PolyhedralConvexShape polyshape = (PolyhedralConvexShape) shape;
                                Vector3f a = Stack.alloc(Vector3f.class);
                                Vector3f b = Stack.alloc(Vector3f.class);
                                for (int i=0;i<polyshape.getNumEdges();i++) {
                                    polyshape.getEdge(i,a,b);
                                    worldTransform.transform(a);
                                    worldTransform.transform(b);
                                    getDebugDrawer().drawLine(a,b,color);
                                }
                            }
                        }
                    }
                }
	}
	
	@Override
	public void setConstraintSolver(ConstraintSolver solver) {
		if (ownsConstraintSolver) {
			//btAlignedFree( m_constraintSolver);
		}
		ownsConstraintSolver = false;
		constraintSolver = solver;
	}

	@Override
	public ConstraintSolver getConstraintSolver() {
		return constraintSolver;
	}

	@Override
	public int getNumConstraints() {
		return constraints.size();
	}

	@Override
	public TypedConstraint getConstraint(int index) {
		return constraints.get(index);
	}
	
	public SimulationIslandManager getSimulationIslandManager() {
		return islandManager;
	}

	public CollisionWorld getCollisionWorld() {
		return this;
	}

	@Override
	public DynamicsWorldType getWorldType() {
		return DynamicsWorldType.DISCRETE_DYNAMICS_WORLD;
	}
	
	////////////////////////////////////////////////////////////////////////////
	
	private static final Comparator<TypedConstraint> sortConstraintOnIslandPredicate = new Comparator<TypedConstraint>() {
		public int compare(TypedConstraint lhs, TypedConstraint rhs) {
			int rIslandId0, lIslandId0;
			rIslandId0 = getConstraintIslandId(rhs);
			lIslandId0 = getConstraintIslandId(lhs);
			return lIslandId0 < rIslandId0? -1 : +1;
		}
	};
	
	private static class DebugDrawcallback implements TriangleCallback, InternalTriangleIndexCallback {
		private IDebugDraw debugDrawer;
		private final Vector3f color = new Vector3f();
		private final Transform worldTrans = new Transform();

		public DebugDrawcallback(IDebugDraw debugDrawer, Transform worldTrans, Vector3f color) {
			this.debugDrawer = debugDrawer;
			this.worldTrans.set(worldTrans);
			this.color.set(color);
		}

		public void internalProcessTriangleIndex(Vector3f[] triangle, int partId, int triangleIndex) {
			processTriangle(triangle,partId,triangleIndex);
		}

		private final Vector3f wv0 = new Vector3f(),wv1 = new Vector3f(),wv2 = new Vector3f();
		
		public void processTriangle(Vector3f[] triangle, int partId, int triangleIndex) {
			wv0.set(triangle[0]);
			worldTrans.transform(wv0);
			wv1.set(triangle[1]);
			worldTrans.transform(wv1);
			wv2.set(triangle[2]);
			worldTrans.transform(wv2);

			debugDrawer.drawLine(wv0, wv1, color);
			debugDrawer.drawLine(wv1, wv2, color);
			debugDrawer.drawLine(wv2, wv0, color);
		}
	}

}

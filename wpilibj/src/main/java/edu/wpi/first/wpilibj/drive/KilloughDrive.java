// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package edu.wpi.first.wpilibj.drive;

import static java.util.Objects.requireNonNull;

import edu.wpi.first.hal.FRCNetComm.tInstances;
import edu.wpi.first.hal.FRCNetComm.tResourceType;
import edu.wpi.first.hal.HAL;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.util.sendable.Sendable;
import edu.wpi.first.util.sendable.SendableBuilder;
import edu.wpi.first.util.sendable.SendableRegistry;
import edu.wpi.first.wpilibj.SpeedController;

/**
 * A class for driving Killough drive platforms.
 *
 * <p>Killough drives are triangular with one omni wheel on each corner.
 *
 * <p>Drive base diagram:
 *
 * <pre>
 *  /_____\
 * / \   / \
 *    \ /
 *    ---
 * </pre>
 *
 * <p>Each drive() function provides different inverse kinematic relations for a Killough drive. The
 * default wheel vectors are parallel to their respective opposite sides, but can be overridden. See
 * the constructor for more information.
 *
 * <p>This library uses the NED axes convention (North-East-Down as external reference in the world
 * frame): http://www.nuclearprojects.com/ins/images/axis_big.png.
 *
 * <p>The positive X axis points ahead, the positive Y axis points right, and the positive Z axis
 * points down. Rotations follow the right-hand rule, so clockwise rotation around the Z axis is
 * positive.
 */
@SuppressWarnings("removal")
public class KilloughDrive extends RobotDriveBase implements Sendable, AutoCloseable {
  public static final double kDefaultLeftMotorAngle = 60.0;
  public static final double kDefaultRightMotorAngle = 120.0;
  public static final double kDefaultBackMotorAngle = 270.0;

  private static int instances;

  private SpeedController m_leftMotor;
  private SpeedController m_rightMotor;
  private SpeedController m_backMotor;

  private Vector2d m_leftVec;
  private Vector2d m_rightVec;
  private Vector2d m_backVec;

  private boolean m_reported;

  /**
   * Wheel speeds for a Killough drive.
   *
   * <p>Uses normalized voltage [-1.0..1.0].
   */
  @SuppressWarnings("MemberName")
  public static class WheelSpeeds {
    public double left;
    public double right;
    public double back;

    /** Constructs a WheelSpeeds with zeroes for left, right, and back speeds. */
    public WheelSpeeds() {}

    /**
     * Constructs a WheelSpeeds.
     *
     * @param left The left speed [-1.0..1.0].
     * @param right The right speed [-1.0..1.0].
     * @param back The back speed [-1.0..1.0].
     */
    public WheelSpeeds(double left, double right, double back) {
      this.left = left;
      this.right = right;
      this.back = back;
    }
  }

  /**
   * Construct a Killough drive with the given motors and default motor angles.
   *
   * <p>The default motor angles make the wheels on each corner parallel to their respective
   * opposite sides.
   *
   * <p>If a motor needs to be inverted, do so before passing it in.
   *
   * @param leftMotor The motor on the left corner.
   * @param rightMotor The motor on the right corner.
   * @param backMotor The motor on the back corner.
   */
  public KilloughDrive(
      SpeedController leftMotor, SpeedController rightMotor, SpeedController backMotor) {
    this(
        leftMotor,
        rightMotor,
        backMotor,
        kDefaultLeftMotorAngle,
        kDefaultRightMotorAngle,
        kDefaultBackMotorAngle);
  }

  /**
   * Construct a Killough drive with the given motors.
   *
   * <p>Angles are measured in degrees clockwise from the positive X axis.
   *
   * @param leftMotor The motor on the left corner.
   * @param rightMotor The motor on the right corner.
   * @param backMotor The motor on the back corner.
   * @param leftMotorAngle The angle of the left wheel's forward direction of travel.
   * @param rightMotorAngle The angle of the right wheel's forward direction of travel.
   * @param backMotorAngle The angle of the back wheel's forward direction of travel.
   */
  public KilloughDrive(
      SpeedController leftMotor,
      SpeedController rightMotor,
      SpeedController backMotor,
      double leftMotorAngle,
      double rightMotorAngle,
      double backMotorAngle) {
    requireNonNull(leftMotor, "Left motor cannot be null");
    requireNonNull(rightMotor, "Right motor cannot be null");
    requireNonNull(backMotor, "Back motor cannot be null");

    m_leftMotor = leftMotor;
    m_rightMotor = rightMotor;
    m_backMotor = backMotor;
    m_leftVec =
        new Vector2d(
            Math.cos(leftMotorAngle * (Math.PI / 180.0)),
            Math.sin(leftMotorAngle * (Math.PI / 180.0)));
    m_rightVec =
        new Vector2d(
            Math.cos(rightMotorAngle * (Math.PI / 180.0)),
            Math.sin(rightMotorAngle * (Math.PI / 180.0)));
    m_backVec =
        new Vector2d(
            Math.cos(backMotorAngle * (Math.PI / 180.0)),
            Math.sin(backMotorAngle * (Math.PI / 180.0)));
    SendableRegistry.addChild(this, m_leftMotor);
    SendableRegistry.addChild(this, m_rightMotor);
    SendableRegistry.addChild(this, m_backMotor);
    instances++;
    SendableRegistry.addLW(this, "KilloughDrive", instances);
  }

  @Override
  public void close() {
    SendableRegistry.remove(this);
  }

  /**
   * Drive method for Killough platform.
   *
   * <p>Angles are measured clockwise from the positive X axis. The robot's speed is independent
   * from its angle or rotation rate.
   *
   * @param ySpeed The robot's speed along the Y axis [-1.0..1.0]. Right is positive.
   * @param xSpeed The robot's speed along the X axis [-1.0..1.0]. Forward is positive.
   * @param zRotation The robot's rotation rate around the Z axis [-1.0..1.0]. Clockwise is
   *     positive.
   */
  @SuppressWarnings("ParameterName")
  public void driveCartesian(double ySpeed, double xSpeed, double zRotation) {
    driveCartesian(ySpeed, xSpeed, zRotation, 0.0);
  }

  /**
   * Drive method for Killough platform.
   *
   * <p>Angles are measured clockwise from the positive X axis. The robot's speed is independent
   * from its angle or rotation rate.
   *
   * @param ySpeed The robot's speed along the Y axis [-1.0..1.0]. Right is positive.
   * @param xSpeed The robot's speed along the X axis [-1.0..1.0]. Forward is positive.
   * @param zRotation The robot's rotation rate around the Z axis [-1.0..1.0]. Clockwise is
   *     positive.
   * @param gyroAngle The current angle reading from the gyro in degrees around the Z axis. Use this
   *     to implement field-oriented controls.
   */
  @SuppressWarnings("ParameterName")
  public void driveCartesian(double ySpeed, double xSpeed, double zRotation, double gyroAngle) {
    if (!m_reported) {
      HAL.report(
          tResourceType.kResourceType_RobotDrive, tInstances.kRobotDrive2_KilloughCartesian, 3);
      m_reported = true;
    }

    ySpeed = MathUtil.applyDeadband(ySpeed, m_deadband);
    xSpeed = MathUtil.applyDeadband(xSpeed, m_deadband);

    var speeds = driveCartesianIK(ySpeed, xSpeed, zRotation, gyroAngle);

    m_leftMotor.set(speeds.left * m_maxOutput);
    m_rightMotor.set(speeds.right * m_maxOutput);
    m_backMotor.set(speeds.back * m_maxOutput);

    feed();
  }

  /**
   * Drive method for Killough platform.
   *
   * <p>Angles are measured counter-clockwise from straight ahead. The speed at which the robot
   * drives (translation) is independent from its angle or rotation rate.
   *
   * @param magnitude The robot's speed at a given angle [-1.0..1.0]. Forward is positive.
   * @param angle The angle around the Z axis at which the robot drives in degrees [-180..180].
   * @param zRotation The robot's rotation rate around the Z axis [-1.0..1.0]. Clockwise is
   *     positive.
   */
  @SuppressWarnings("ParameterName")
  public void drivePolar(double magnitude, double angle, double zRotation) {
    if (!m_reported) {
      HAL.report(tResourceType.kResourceType_RobotDrive, tInstances.kRobotDrive2_KilloughPolar, 3);
      m_reported = true;
    }

    driveCartesian(
        magnitude * Math.sin(angle * (Math.PI / 180.0)),
        magnitude * Math.cos(angle * (Math.PI / 180.0)),
        zRotation,
        0.0);
  }

  /**
   * Cartesian inverse kinematics for Killough platform.
   *
   * <p>Angles are measured clockwise from the positive X axis. The robot's speed is independent
   * from its angle or rotation rate.
   *
   * @param ySpeed The robot's speed along the Y axis [-1.0..1.0]. Right is positive.
   * @param xSpeed The robot's speed along the X axis [-1.0..1.0]. Forward is positive.
   * @param zRotation The robot's rotation rate around the Z axis [-1.0..1.0]. Clockwise is
   *     positive.
   * @param gyroAngle The current angle reading from the gyro in degrees around the Z axis. Use this
   *     to implement field-oriented controls.
   * @return Wheel speeds [-1.0..1.0].
   */
  @SuppressWarnings("ParameterName")
  public WheelSpeeds driveCartesianIK(
      double ySpeed, double xSpeed, double zRotation, double gyroAngle) {
    ySpeed = MathUtil.clamp(ySpeed, -1.0, 1.0);
    xSpeed = MathUtil.clamp(xSpeed, -1.0, 1.0);

    // Compensate for gyro angle.
    Vector2d input = new Vector2d(ySpeed, xSpeed);
    input.rotate(-gyroAngle);

    double[] wheelSpeeds = new double[3];
    wheelSpeeds[MotorType.kLeft.value] = input.scalarProject(m_leftVec) + zRotation;
    wheelSpeeds[MotorType.kRight.value] = input.scalarProject(m_rightVec) + zRotation;
    wheelSpeeds[MotorType.kBack.value] = input.scalarProject(m_backVec) + zRotation;

    normalize(wheelSpeeds);

    return new WheelSpeeds(
        wheelSpeeds[MotorType.kLeft.value],
        wheelSpeeds[MotorType.kRight.value],
        wheelSpeeds[MotorType.kBack.value]);
  }

  @Override
  public void stopMotor() {
    m_leftMotor.stopMotor();
    m_rightMotor.stopMotor();
    m_backMotor.stopMotor();
    feed();
  }

  @Override
  public String getDescription() {
    return "KilloughDrive";
  }

  @Override
  public void initSendable(SendableBuilder builder) {
    builder.setSmartDashboardType("KilloughDrive");
    builder.setActuator(true);
    builder.setSafeState(this::stopMotor);
    builder.addDoubleProperty("Left Motor Speed", m_leftMotor::get, m_leftMotor::set);
    builder.addDoubleProperty("Right Motor Speed", m_rightMotor::get, m_rightMotor::set);
    builder.addDoubleProperty("Back Motor Speed", m_backMotor::get, m_backMotor::set);
  }
}

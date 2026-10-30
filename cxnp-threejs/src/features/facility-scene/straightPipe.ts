import * as THREE from "three";

export type PipePoint = readonly [x: number, z: number];

export interface StraightPipeTransform {
  length: number;
  midpoint: [number, number, number];
  quaternion: THREE.Quaternion;
}

export interface StraightPipeInstanceTransform extends StraightPipeTransform {
  matrix: THREE.Matrix4;
}

const CYLINDER_AXIS = new THREE.Vector3(0, 1, 0);

/**
 * Converts two ground-plane endpoints into the transform used by a cylinder.
 * Three.js cylinders extend along local Y, so the quaternion rotates that axis
 * onto the requested pipe direction in the XZ plane.
 */
export function getStraightPipeTransform(
  from: PipePoint,
  to: PipePoint,
  elevation = -0.14,
): StraightPipeTransform {
  const direction = new THREE.Vector3(to[0] - from[0], 0, to[1] - from[1]);
  const length = direction.length();

  if (length === 0) {
    throw new Error("Straight pipe endpoints must not be identical.");
  }

  return {
    length,
    midpoint: [(from[0] + to[0]) / 2, elevation, (from[1] + to[1]) / 2],
    quaternion: new THREE.Quaternion().setFromUnitVectors(CYLINDER_AXIS, direction.normalize()),
  };
}

/**
 * Builds the per-instance matrix for a unit cylinder whose local Y length is 1.
 * The geometry and material can therefore stay shared by every pipe instance.
 */
export function getStraightPipeInstanceTransform(
  from: PipePoint,
  to: PipePoint,
  radius: number,
  elevation = -0.14,
): StraightPipeInstanceTransform {
  const transform = getStraightPipeTransform(from, to, elevation);
  // 位置，旋转，缩放三个参数
  const matrix = new THREE.Matrix4().compose(
    new THREE.Vector3(...transform.midpoint),
    transform.quaternion,
    new THREE.Vector3(radius, transform.length, radius),
  );

  return { ...transform, matrix };
}

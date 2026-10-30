import * as THREE from "three";

export type CurvedPipePoint = readonly [x: number, z: number];

/**
 * Builds a smooth ground-plane path for TubeGeometry. Unlike a cylinder,
 * TubeGeometry sweeps its circular cross-section along this complete curve.
 */
export function createCurvedPipePath(
  points: readonly CurvedPipePoint[],
  elevation = -0.14,
) {
  if (points.length < 3) {
    throw new Error("A curved pipe needs at least three control points.");
  }

  for (let index = 1; index < points.length; index += 1) {
    if (points[index][0] === points[index - 1][0] && points[index][1] === points[index - 1][1]) {
      throw new Error("Adjacent curved pipe control points must be different.");
    }
  }

  return new THREE.CatmullRomCurve3(
    points.map(([x, z]) => new THREE.Vector3(x, elevation, z)),
    false,
    "centripetal",
  );
}

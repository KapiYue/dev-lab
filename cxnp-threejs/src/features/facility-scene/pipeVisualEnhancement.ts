import * as THREE from "three";

export const FLOW_LINE_OFFSETS = [
  [0, 0],
  [-0.34, -0.18],
  [0.34, 0.18],
  [-0.2, 0.34],
  [0.2, -0.34],
] as const;

export function createOffsetPipePath(
  path: THREE.Curve<THREE.Vector3>,
  radius: number,
  lateralFactor: number,
  verticalFactor: number,
  samples = 48,
) {
  if (!Number.isFinite(radius) || radius <= 0) {
    throw new RangeError("Pipe radius must be finite and positive.");
  }
  if (![lateralFactor, verticalFactor].every(Number.isFinite)) {
    throw new RangeError("Flow line offsets must be finite.");
  }
  if (!Number.isInteger(samples) || samples < 2) {
    throw new RangeError("Offset path samples must be an integer of at least two.");
  }

  const tangent = new THREE.Vector3();
  const reference = new THREE.Vector3();
  const normalA = new THREE.Vector3();
  const normalB = new THREE.Vector3();
  const points = Array.from({ length: samples + 1 }, (_, index) => {
    const progress = index / samples;
    const point = path.getPointAt(progress);
    path.getTangentAt(progress, tangent).normalize();
    reference.set(Math.abs(tangent.y) > 0.9 ? 1 : 0, Math.abs(tangent.y) > 0.9 ? 0 : 1, 0);
    normalA.copy(tangent).cross(reference).normalize();
    normalB.copy(tangent).cross(normalA).normalize();
    return point
      .addScaledVector(normalA, radius * lateralFactor)
      .addScaledVector(normalB, radius * verticalFactor);
  });

  return new THREE.CatmullRomCurve3(points, false, "centripetal");
}

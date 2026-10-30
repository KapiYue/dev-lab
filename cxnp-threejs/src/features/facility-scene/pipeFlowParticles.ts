import * as THREE from "three";

export const PARTICLE_PROGRESS_SPEED_SCALE = 0.3;

export function createParticlePhases(count: number) {
  if (!Number.isInteger(count) || count <= 0) {
    throw new RangeError("Particle count must be a positive integer.");
  }

  return Float32Array.from({ length: count }, (_, index) => index / count);
}

export function createParticleRadialOffsets(count: number, radius: number) {
  if (!Number.isInteger(count) || count <= 0) {
    throw new RangeError("Particle count must be a positive integer.");
  }
  if (!Number.isFinite(radius) || radius < 0) {
    throw new RangeError("Particle stream radius must be finite and non-negative.");
  }

  const offsets = new Float32Array(count * 2);
  const goldenAngle = Math.PI * (3 - Math.sqrt(5));
  for (let index = 0; index < count; index += 1) {
    const shuffledIndex = (index * 37) % count;
    const distance = Math.sqrt((shuffledIndex + 0.5) / count) * radius;
    const angle = index * goldenAngle;
    offsets[index * 2] = Math.cos(angle) * distance;
    offsets[index * 2 + 1] = Math.sin(angle) * distance;
  }

  return offsets;
}

export function advanceParticlePhase(
  phase: number,
  deltaSeconds: number,
  speed: number,
) {
  if (![phase, deltaSeconds, speed].every(Number.isFinite) || deltaSeconds < 0 || speed < 0) {
    throw new RangeError("Particle animation values must be finite and non-negative.");
  }

  return (phase + deltaSeconds * speed * PARTICLE_PROGRESS_SPEED_SCALE) % 1;
}

export function writeParticlePositions(
  path: THREE.Curve<THREE.Vector3>,
  phases: Float32Array,
  phaseOffset: number,
  positions: Float32Array,
  radialOffsets?: Float32Array,
) {
  if (positions.length !== phases.length * 3) {
    throw new RangeError("Particle position buffer must contain three values per phase.");
  }
  if (radialOffsets && radialOffsets.length !== phases.length * 2) {
    throw new RangeError("Particle radial buffer must contain two values per phase.");
  }

  const point = new THREE.Vector3();
  const tangent = new THREE.Vector3();
  const normalA = new THREE.Vector3();
  const normalB = new THREE.Vector3();
  const reference = new THREE.Vector3();
  phases.forEach((phase, index) => {
    const progress = (phase + phaseOffset) % 1;
    path.getPointAt(progress, point);
    if (radialOffsets) {
      path.getTangentAt(progress, tangent).normalize();
      reference.set(Math.abs(tangent.y) > 0.9 ? 1 : 0, Math.abs(tangent.y) > 0.9 ? 0 : 1, 0);
      normalA.copy(tangent).cross(reference).normalize();
      normalB.copy(tangent).cross(normalA).normalize();
      point.addScaledVector(normalA, radialOffsets[index * 2]);
      point.addScaledVector(normalB, radialOffsets[index * 2 + 1]);
    }
    positions[index * 3] = point.x;
    positions[index * 3 + 1] = point.y;
    positions[index * 3 + 2] = point.z;
  });

  return positions;
}

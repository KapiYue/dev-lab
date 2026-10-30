import { describe, expect, it } from "vitest";
import * as THREE from "three";
import {
  PARTICLE_PROGRESS_SPEED_SCALE,
  advanceParticlePhase,
  createParticlePhases,
  createParticleRadialOffsets,
  writeParticlePositions,
} from "./pipeFlowParticles";

describe("pipe flow particles", () => {
  it("distributes particles evenly along a pipe", () => {
    expect(Array.from(createParticlePhases(4))).toEqual([0, 0.25, 0.5, 0.75]);
  });

  it("advances with flow speed and wraps at the pipe end", () => {
    expect(advanceParticlePhase(0.2, 1, 0)).toBe(0.2);
    expect(advanceParticlePhase(0.9, 1, 1)).toBeCloseTo(
      (0.9 + PARTICLE_PROGRESS_SPEED_SCALE) % 1,
    );
    expect(advanceParticlePhase(0.1, 0.5, 0.8)).toBeCloseTo(0.22);
  });

  it("writes positions from the requested path direction", () => {
    const path = new THREE.LineCurve3(
      new THREE.Vector3(-2, -0.14, 1),
      new THREE.Vector3(2, -0.14, 1),
    );
    const positions = writeParticlePositions(
      path,
      createParticlePhases(2),
      0.25,
      new Float32Array(6),
    );

    expect(Array.from(positions)).toEqual([-1, -0.14000000059604645, 1, 1, -0.14000000059604645, 1]);
  });

  it("supports curved pipe paths and validates invalid buffers", () => {
    const path = new THREE.QuadraticBezierCurve3(
      new THREE.Vector3(0, 0, 0),
      new THREE.Vector3(1, 0, 2),
      new THREE.Vector3(2, 0, 0),
    );
    const positions = writeParticlePositions(
      path,
      createParticlePhases(3),
      0,
      new Float32Array(9),
    );

    expect(positions[0]).toBe(0);
    expect(positions[2]).toBe(0);
    expect(positions[5]).toBeGreaterThan(0);
    expect(() => writeParticlePositions(path, createParticlePhases(2), 0, new Float32Array(3))).toThrow(
      "Particle position buffer must contain three values per phase.",
    );
    expect(() => createParticlePhases(0)).toThrow("Particle count must be a positive integer.");
  });

  it("spreads fine particles across the pipe cross-section", () => {
    const path = new THREE.LineCurve3(
      new THREE.Vector3(0, -2, 0),
      new THREE.Vector3(0, 2, 0),
    );
    const phases = createParticlePhases(12);
    const radialOffsets = createParticleRadialOffsets(12, 0.4);
    const positions = writeParticlePositions(
      path,
      phases,
      0,
      new Float32Array(36),
      radialOffsets,
    );

    const hasCrossSectionSpread = positions.some((value, index) => index % 3 !== 1 && Math.abs(value) > 0.01);
    expect(hasCrossSectionSpread).toBe(true);
    expect(Math.max(...positions.filter((_, index) => index % 3 !== 1).map(Math.abs))).toBeLessThanOrEqual(0.4);
    expect(() => writeParticlePositions(path, phases, 0, positions, new Float32Array(3))).toThrow(
      "Particle radial buffer must contain two values per phase.",
    );
  });
});

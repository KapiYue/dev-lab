import { describe, expect, it } from "vitest";
import * as THREE from "three";
import { getStraightPipeInstanceTransform, getStraightPipeTransform } from "./straightPipe";

describe("getStraightPipeTransform", () => {
  it("derives the midpoint and length from the pipe endpoints", () => {
    const transform = getStraightPipeTransform([-4.7, 0.7], [0.5, -1.1]);

    expect(transform.length).toBeCloseTo(Math.hypot(5.2, -1.8));
    expect(transform.midpoint[0]).toBeCloseTo(-2.1);
    expect(transform.midpoint[1]).toBeCloseTo(-0.14);
    expect(transform.midpoint[2]).toBeCloseTo(-0.2);
  });

  it("rotates the cylinder Y axis onto the pipe direction", () => {
    const transform = getStraightPipeTransform([1, 2], [4, 6]);
    const rotatedAxis = new THREE.Vector3(0, 1, 0).applyQuaternion(transform.quaternion);
    const expectedDirection = new THREE.Vector3(3, 0, 4).normalize();

    expect(rotatedAxis.distanceTo(expectedDirection)).toBeLessThan(1e-10);
  });

  it("rejects a zero-length pipe", () => {
    expect(() => getStraightPipeTransform([2, 3], [2, 3])).toThrow(
      "Straight pipe endpoints must not be identical.",
    );
  });
});

describe("getStraightPipeInstanceTransform", () => {
  it("encodes independent position, direction, length and radius in a matrix", () => {
    const transform = getStraightPipeInstanceTransform([1, 2], [4, 6], 0.5, -0.2);
    const position = new THREE.Vector3();
    const quaternion = new THREE.Quaternion();
    const scale = new THREE.Vector3();

    transform.matrix.decompose(position, quaternion, scale);

    expect(position.toArray()).toEqual([2.5, -0.2, 4]);
    expect(scale.x).toBeCloseTo(0.5);
    expect(scale.y).toBeCloseTo(5);
    expect(scale.z).toBeCloseTo(0.5);
    expect(new THREE.Vector3(0, 1, 0).applyQuaternion(quaternion).distanceTo(
      new THREE.Vector3(3, 0, 4).normalize(),
    )).toBeLessThan(1e-10);
  });

  it("creates different matrices for pipes with different transforms", () => {
    const first = getStraightPipeInstanceTransform([0, 0], [2, 0], 0.4);
    const second = getStraightPipeInstanceTransform([3, 1], [3, 5], 0.7);

    expect(first.matrix.equals(second.matrix)).toBe(false);
    expect(first.length).toBeCloseTo(2);
    expect(second.length).toBeCloseTo(4);
  });
});

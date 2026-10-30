import { describe, expect, it } from "vitest";
import * as THREE from "three";
import { FLOW_LINE_OFFSETS, createOffsetPipePath } from "./pipeVisualEnhancement";

describe("pipe visual enhancement paths", () => {
  it("creates five distinct flow lines inside the pipe radius", () => {
    const source = new THREE.LineCurve3(
      new THREE.Vector3(0, -2, 0),
      new THREE.Vector3(0, 2, 0),
    );
    const paths = FLOW_LINE_OFFSETS.map(([lateral, vertical]) => (
      createOffsetPipePath(source, 0.5, lateral, vertical)
    ));
    const centers = paths.map((path) => path.getPointAt(0.5));

    expect(paths).toHaveLength(5);
    expect(new Set(centers.map((point) => point.toArray().map((value) => value.toFixed(3)).join(","))).size).toBe(5);
    centers.forEach((point) => expect(Math.hypot(point.x, point.z)).toBeLessThan(0.5));
  });

  it("keeps offset flow lines parallel to a curved source path", () => {
    const source = new THREE.QuadraticBezierCurve3(
      new THREE.Vector3(0, 0, 0),
      new THREE.Vector3(1, 0, 2),
      new THREE.Vector3(3, 0, 1),
    );
    const offset = createOffsetPipePath(source, 0.4, 0.3, -0.2, 32);

    expect(offset.getPointAt(0).distanceTo(source.getPointAt(0))).toBeLessThan(0.2);
    expect(offset.getPointAt(1).distanceTo(source.getPointAt(1))).toBeLessThan(0.2);
    expect(offset.getPointAt(0.5).y).not.toBe(0);
  });

  it("rejects invalid radii, offsets, and sample counts", () => {
    const source = new THREE.LineCurve3(new THREE.Vector3(), new THREE.Vector3(1, 0, 0));
    expect(() => createOffsetPipePath(source, 0, 0, 0)).toThrow(RangeError);
    expect(() => createOffsetPipePath(source, 1, Number.NaN, 0)).toThrow(RangeError);
    expect(() => createOffsetPipePath(source, 1, 0, 0, 1)).toThrow(RangeError);
  });
});

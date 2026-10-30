import { describe, expect, it } from "vitest";
import * as THREE from "three";
import { createCurvedPipePath } from "./curvedPipe";

describe("createCurvedPipePath", () => {
  it("keeps the requested endpoints and elevation", () => {
    const path = createCurvedPipePath([[1, 2], [2, 4], [5, 3]], -0.2);

    expect(path.getPoint(0).toArray()).toEqual([1, -0.2, 2]);
    expect(path.getPoint(1).toArray()).toEqual([5, -0.2, 3]);
    expect(path.getPoint(0.5).y).toBeCloseTo(-0.2);
  });

  it("produces a curved TubeGeometry instead of a straight cylinder transform", () => {
    const path = createCurvedPipePath([[0, 0], [1, 2], [3, 2], [4, 0]]);
    const geometry = new THREE.TubeGeometry(path, 32, 0.3, 12, false);
    const center = path.getPoint(0.5);

    expect(center.z).toBeGreaterThan(1.5);
    expect(geometry.parameters.path).toBe(path);
    expect(geometry.parameters.tubularSegments).toBe(32);
    expect(geometry.parameters.closed).toBe(false);
  });

  it("rejects an incomplete path and repeated neighboring points", () => {
    expect(() => createCurvedPipePath([[0, 0], [1, 1]])).toThrow(
      "A curved pipe needs at least three control points.",
    );
    expect(() => createCurvedPipePath([[0, 0], [1, 1], [1, 1]])).toThrow(
      "Adjacent curved pipe control points must be different.",
    );
  });
});

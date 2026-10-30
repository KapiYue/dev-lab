import { describe, expect, it } from "vitest";
import * as THREE from "three";
import {
  PIPE_INSTANCE_VISUALS,
  applyPipeInstanceStates,
  setPipeInstanceStateAt,
  type PipeInstanceStatus,
} from "./pipeInstanceState";

function createMeshes(count: number) {
  const geometry = new THREE.CylinderGeometry(1, 1, 1);
  const material = new THREE.MeshBasicMaterial({ color: "#ffffff" });
  return {
    shell: new THREE.InstancedMesh(geometry, material, count),
    flow: new THREE.InstancedMesh(geometry, material.clone(), count),
  };
}

function readColor(mesh: THREE.InstancedMesh, index: number) {
  const color = new THREE.Color();
  mesh.getColorAt(index, color);
  return `#${color.getHexString()}`;
}

describe("pipe instance states", () => {
  it("assigns an independent shell and flow color to every state", () => {
    const statuses: PipeInstanceStatus[] = ["normal", "warning", "critical", "stopped"];
    const { shell, flow } = createMeshes(statuses.length);

    applyPipeInstanceStates(shell, flow, statuses);

    statuses.forEach((status, index) => {
      expect(readColor(shell, index)).toBe(PIPE_INSTANCE_VISUALS[status].shellColor);
      expect(readColor(flow, index)).toBe(PIPE_INSTANCE_VISUALS[status].flowColor);
    });
  });

  it("updates one instance without synchronizing the color to its neighbors", () => {
    const { shell, flow } = createMeshes(3);
    applyPipeInstanceStates(shell, flow, ["normal", "normal", "normal"]);
    const firstBefore = readColor(flow, 0);
    const thirdBefore = readColor(flow, 2);

    setPipeInstanceStateAt(shell, flow, 1, "critical");

    expect(readColor(flow, 0)).toBe(firstBefore);
    expect(readColor(flow, 1)).toBe(PIPE_INSTANCE_VISUALS.critical.flowColor);
    expect(readColor(flow, 2)).toBe(thirdBefore);
  });

  it("rejects status lists and indexes that do not match the instance meshes", () => {
    const { shell, flow } = createMeshes(2);

    expect(() => applyPipeInstanceStates(shell, flow, ["normal"])).toThrow(
      "Pipe status count must match both InstancedMesh counts.",
    );
    expect(() => setPipeInstanceStateAt(shell, flow, 2, "warning")).toThrow(RangeError);
  });
});

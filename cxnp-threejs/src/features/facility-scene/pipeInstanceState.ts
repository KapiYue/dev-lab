import * as THREE from "three";

export type PipeInstanceStatus = "normal" | "warning" | "critical" | "stopped";

export interface PipeInstanceVisual {
  label: string;
  shellColor: string;
  flowColor: string;
}

export const PIPE_INSTANCE_VISUALS: Record<PipeInstanceStatus, PipeInstanceVisual> = {
  normal: { label: "正常", shellColor: "#4ca8ba", flowColor: "#1aa9d6" },
  warning: { label: "预警", shellColor: "#b77d2e", flowColor: "#ffb84d" },
  critical: { label: "异常", shellColor: "#c9474f", flowColor: "#ff4050" },
  stopped: { label: "停止", shellColor: "#546774", flowColor: "#71818a" },
};

/**
 * Writes colors to one instance only. The shared material remains white so the
 * instance color is the sole source of each pipe's visible status color.
 */
export function setPipeInstanceStateAt(
  shellMesh: THREE.InstancedMesh,
  flowMesh: THREE.InstancedMesh,
  index: number,
  status: PipeInstanceStatus,
) {
  if (index < 0 || index >= shellMesh.count || index >= flowMesh.count) {
    throw new RangeError(`Pipe instance index ${index} is out of range.`);
  }

  const visual = PIPE_INSTANCE_VISUALS[status];
  shellMesh.setColorAt(index, new THREE.Color(visual.shellColor));
  flowMesh.setColorAt(index, new THREE.Color(visual.flowColor));
  if (shellMesh.instanceColor) shellMesh.instanceColor.needsUpdate = true;
  if (flowMesh.instanceColor) flowMesh.instanceColor.needsUpdate = true;
}

export function applyPipeInstanceStates(
  shellMesh: THREE.InstancedMesh,
  flowMesh: THREE.InstancedMesh,
  statuses: readonly PipeInstanceStatus[],
) {
  if (statuses.length !== shellMesh.count || statuses.length !== flowMesh.count) {
    throw new Error("Pipe status count must match both InstancedMesh counts.");
  }

  statuses.forEach((status, index) => {
    setPipeInstanceStateAt(shellMesh, flowMesh, index, status);
  });
}

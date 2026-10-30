import type { AlarmLevel, FacilityStatus } from "../../types/domain";
import type { PipeFlowStatus } from "./pipeFlowShader";

export const FLOW_TO_SHADER_SPEED = 0.2;
export const MAX_SHADER_FLOW_SPEED = 1.2;

export interface MonitoringPipeFlowInput {
  instantFlow: number;
  pumpRunning: boolean;
  facilityStatus: FacilityStatus;
  alarmLevel: AlarmLevel;
}

export interface MonitoringPipeFlowVisual {
  status: PipeFlowStatus;
  speed: number;
  flowing: boolean;
}

/**
 * Converts the current monitoring snapshot into the small set of values used by
 * the pipe shader. Keeping this mapping outside React makes the data boundary
 * explicit and lets a future REST/WebSocket source reuse the same rules.
 */
export function mapMonitoringToPipeFlow({
  instantFlow,
  pumpRunning,
  facilityStatus,
  alarmLevel,
}: MonitoringPipeFlowInput): MonitoringPipeFlowVisual {
  const offline = facilityStatus === "offline" || alarmLevel === "offline";
  const status: PipeFlowStatus = offline
    ? "offline"
    : alarmLevel === "critical"
      ? "critical"
      : alarmLevel === "warning" || facilityStatus === "warning"
        ? "warning"
        : "normal";
  const validFlow = Number.isFinite(instantFlow) ? Math.max(0, instantFlow) : 0;
  const flowing = !offline && pumpRunning && validFlow > 0;
  const speed = flowing
    ? Math.min(MAX_SHADER_FLOW_SPEED, validFlow * FLOW_TO_SHADER_SPEED)
    : 0;

  return { status, speed, flowing };
}
